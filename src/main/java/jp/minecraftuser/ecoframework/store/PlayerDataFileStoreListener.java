
package jp.minecraftuser.ecoframework.store;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import jp.minecraftuser.ecoframework.ListenerFrame;
import jp.minecraftuser.ecoframework.PluginFrame;
import static jp.minecraftuser.ecoframework.Utl.sendPluginMessage;
import jp.minecraftuser.ecoframework.exception.DatabaseControlException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * playerdata DB保管クラス
 * @author ecolight
 */
public class PlayerDataFileStoreListener extends ListenerFrame {
    private String db;
    private String dbname;
    private String dbserver;
    private String dbuser;
    private String dbpass;
    private HashMap<UUID, PlayerDataFileStoreAsyncThread> workTable = new HashMap<>();
    /**
     * コンストラクタ
     * @param plg_ プラグインフレームインスタンス
     * @param name_ 名前
     */
    public PlayerDataFileStoreListener(PluginFrame plg_, String name_) {
        super(plg_, name_);
        // リロード対応
        conf.registerNotifiable(this);
        reloadNotify();
    }

    /**
     * リロード通知受信
     */
    @Override
    public void reloadNotify() {
        // コンフィグが情報更新しているので読み直す
        db = conf.getString("userdatadb.db");
        dbname = conf.getString("userdatadb.name");
        dbserver = conf.getString("userdatadb.server");
        dbuser = conf.getString("userdatadb.user");
        dbpass = conf.getString("userdatadb.pass");
    }

    /**
     * プレイヤー切断処理
     * プレイヤー切断のタイミングでplayerdataに保存されるプレイヤーの情報を
     * 保存する。ただし、本イベント呼び出し時にはまだ更新されていないため、
     * 現時点から一定時間更新状態を監視する必要がある。
     * 現時点で該当プレイヤーのloginを抑止、ファイルの更新を監視、ファイル更新後
     * にdatabaseへ格納、格納完了でlogin抑止を解除する。
     * 同一プレイヤーが多重ログインしてきた場合のイベント発生順序は次の通り。
     * (A)Kick->(A)Quit->(B)AsyncPreLogin->(B)PreLogin->(B)Login->(B)Join
     * @param event イベント
     */
    @EventHandler
    public void PlayerQuitEvent(PlayerQuitEvent event) {
        // プレイヤーログイン抑止は監視スレッドの存在チェックで行う
        // 非同期スレッドを起こしてプレイヤーデータ監視とDBへの書き込みを依頼する
        UUID uid = event.getPlayer().getUniqueId();
        log.info("start PlayerData stored.");
        // 切断処理中に切断処理が走るはずないが、ガードしておく。
        if (workTable.containsKey(uid)) {
            return;
        }
        
        // 生成と起動 (5tickに1回結果確認する)
        PlayerDataFileStoreAsyncThread t = new PlayerDataFileStoreAsyncThread(plg, this, uid.toString());
        t.runTaskTimer(plg, 0, 5);
        workTable.put(uid, t);

        // ログアウト開始をDB反映しておく
        loadPlayerData(event.getPlayer(), OPE.START);
        
        // ファイルの保存時間を見るため、送信データは先に生成して時刻を保持させる
        PlayerDataFileStorePayload data = new PlayerDataFileStorePayload(plg, uid);

        // DB書き込み前にプレイヤーデータセーブ
        //event.getPlayer().saveData();
        // 個人のセーブだと統計の保存が動作しないため全プレイヤーセーブを呼び出すspigotめ…
        Bukkit.savePlayers();

        // データの作成と依頼
        t.sendData(data);
    }

    /**
     * プレイヤーログイン抑止処理
     * プレイヤーのログイン拒否を含むため、まず優先度LOWSTで抑止処理をしておく
     * @param event イベント
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void RejectPlayer(PlayerLoginEvent event) {
        // 他のプライオリティの処理でログインが抑止されている場合何もしない
        if (!event.getResult().equals(PlayerLoginEvent.Result.ALLOWED)) {
            return;
        }
        
        // プレイヤーデータの退避処理中の場合はログインをガードする
        // サーバー単体の場合はこれでガードできるが、複数サーバーからの排他を行う場合にはDBで排他させる必要がある
        UUID uid = event.getPlayer().getUniqueId();
        if (workTable.containsKey(uid)) {
            // 存在する場合はNG
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "サーバーからのログアウト処理中です\nこの状態が1分以上継続する場合には管理者に相談してください");
            return;
        }

        // DBによるガード
        if (!loadPlayerData(event.getPlayer(), OPE.ISLOGOUT)) {
            // false : ログアウトしていない = 処理中ならガードする
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "連携サーバーからのログアウト処理中です\nこの状態が1分以上継続する場合には管理者に相談してください");
            return;
        }
    }

    /**
     * プレイヤーログイン処理
     * @param event イベント
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void PlayerLoginEvent(PlayerLoginEvent event) {
        // 他のプライオリティの処理でログインが抑止されている場合何もしない
        if (!event.getResult().equals(PlayerLoginEvent.Result.ALLOWED)) {
            return;
        }
        
        // ログイン許可されている場合はDBからプレイヤーデータのロードを行う
        // 取り出しは同期処理で
        log.info("start PlayerData load.");
        
        // データベースからの取り出し
        loadPlayerData(event.getPlayer(), OPE.LOAD);
    }
            
    /**
     * 保存完了処理
     * @param uid プレイヤーUUID
     */
    protected void complete(UUID uid) {
        // DB保存が完了したのでスレッドの停止と監視タイマの停止
        // 作業テーブルからの削除
        PlayerDataFileStoreAsyncThread t = workTable.get(uid);
        t.cancel();
        workTable.remove(uid);
    }
    private enum OPE {
        LOAD, START, ISLOGOUT
    }
    /**
     * プレイヤーデータロード
     * @param p プレイヤーインスタンス
     */
    private boolean loadPlayerData(Player p, OPE ope) {
        PlayerFileStore store = null;
        boolean result = true;
        File datafile = new File(plg.getServer().getWorlds().get(0).getName() + "/playerdata/" + p.getUniqueId().toString() + ".dat");
        try {
            if (db.equalsIgnoreCase("sqlite")) {
                store = new PlayerFileStore(plg, dbname, "playerdata");
            } else if (db.equalsIgnoreCase("mysql")) {
                store = new PlayerFileStore(plg, dbserver, dbuser, dbpass, dbname, "playerdata");
            } else {
                throw new DatabaseControlException("データベース指定が異常です");
            }
            if (store.existPlayerData(p.getUniqueId())) {
                switch (ope) {
                    case LOAD:
                        store.loadPlayerData(p.getUniqueId(), datafile);
                        break;
                    case START:
                        store.logoutPlayer(p.getUniqueId());
                        break;
                    case ISLOGOUT:
                        result = store.isPlayerAfterLogout(p.getUniqueId());
                        break;
                }
            } else {
                log.log(Level.INFO, "not stored PlayerData:{0}", p.getName());
            }
            store.commit();
        } catch (ClassNotFoundException | SQLException | IOException ex) {
            if (store != null) {
                try {
                    store.rollback();
                } catch (SQLException ex1) {
                    Logger.getLogger(PlayerDataFileStoreListener.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
            Logger.getLogger(PlayerDataFileStoreListener.class.getName()).log(Level.SEVERE, null, ex);
        } catch (DatabaseControlException ex) {
            Logger.getLogger(PlayerDataFileStoreListener.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (store != null) {
            store.close();
        }        
        return result;
    }

}

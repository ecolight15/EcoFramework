
package jp.minecraftuser.ecoframework.store;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import jp.minecraftuser.ecoframework.ListenerFrame;
import jp.minecraftuser.ecoframework.PluginFrame;
import jp.minecraftuser.ecoframework.plugin.EcoFrameworkConfig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * playerdata DB保管クラス
 * @author ecolight
 */
public class PlayerDataFileStoreListener extends ListenerFrame {
    private HashMap<UUID, PlayerDataFileStoreAsyncThread> workTable = new HashMap<>();
    private EcoFrameworkConfig efconf;
    /**
     * コンストラクタ
     * @param plg_ プラグインフレームインスタンス
     * @param name_ 名前
     */
    public PlayerDataFileStoreListener(PluginFrame plg_, String name_) {
        super(plg_, name_);
        efconf = (EcoFrameworkConfig) conf;
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
        log.info("Start player quit : " + event.getPlayer().getDisplayName() + " : " + event.getPlayer().getPlayer().getUniqueId().toString());
        // プレイヤーデータ保存未使用なら何もせず終わる
        if (!efconf.dbuse) return;
        // DB接続できていない状況でまだ動作中なら何もしないで終わる
        if (efconf.store == null) return;
        
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
        loadPlayerData(event.getPlayer().getUniqueId(), OPE.START);
        
        // ファイルの保存時間を見るため、送信データは先に生成して時刻を保持させる
        PlayerDataFileStorePayload data = new PlayerDataFileStorePayload(plg, uid);

        // DB書き込み前にプレイヤーデータセーブ
        event.getPlayer().saveData();
        // 個人のセーブだと統計の保存が動作しないため全プレイヤーセーブを呼び出すspigotめ…
        //Bukkit.savePlayers();

        // データの作成と依頼
        t.sendData(data);
        log.info("End player quit : " + event.getPlayer().getDisplayName() + " : " + event.getPlayer().getPlayer().getUniqueId().toString());
    }

    /**
     * プレイヤーログイン抑止処理
     * プレイヤーのログイン拒否を含むため、まず優先度LOWSTで抑止処理をしておく
     * @param event イベント
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void PreLoginRejectPlayer(AsyncPlayerPreLoginEvent event) {
        log.info("Check player reject : " + event.getName() + " : " + event.getName());
        // プレイヤーデータ保存未使用なら何もせず終わる
        if (!efconf.dbuse) return;
        // DB接続できていない状況でまだ動作中なら何もしないで終わる
        if (efconf.store == null) return;

        // 他のプライオリティの処理でログインが抑止されている場合何もしない
        if (!event.getLoginResult().equals(AsyncPlayerPreLoginEvent.Result.ALLOWED)) {
            return;
        }
        
        // プレイヤーデータの退避処理中の場合はログインをガードする
        // サーバー単体の場合はこれでガードできるが、複数サーバーからの排他を行う場合にはDBで排他させる必要がある
        UUID uid = event.getUniqueId();
        if (workTable.containsKey(uid)) {
            // 存在する場合はNG
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "サーバーからのログアウト処理中です\nこの状態が1分以上継続する場合には管理者に相談してください");
            return;
        }

        // DBによるガード
        if (!loadPlayerData(event.getUniqueId(), OPE.ISLOGOUT)) {
            // false : ログアウトしていない = 処理中ならガードする
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "連携サーバーからのログアウト処理中です\nこの状態が1分以上継続する場合には管理者に相談してください");
            return;
        }
    }

    /**
     * プレイヤーログイン処理
     * @param event イベント
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void PlayerPreLoginEvent(AsyncPlayerPreLoginEvent event) {
        log.info("Load player data : " + event.getName() + " : " + event.getName());
        // プレイヤーデータ保存未使用なら何もせず終わる
        if (!efconf.dbuse) return;
        // DB接続できていない状況でまだ動作中なら何もしないで終わる
        if (efconf.store == null) return;

        // 他のプライオリティの処理でログインが抑止されている場合何もしない
        if (!event.getLoginResult().equals(AsyncPlayerPreLoginEvent.Result.ALLOWED)) {
            return;
        }
        
        // ログイン許可されている場合はDBからプレイヤーデータのロードを行う
        // 取り出しは同期処理でデータベースからの取り出し
        log.info("start PlayerData load.");
        loadPlayerData(event.getUniqueId(), OPE.LOAD);
        log.info("Load player data complete : " + event.getName() + " : " + event.getName());
    }
          
    /**
     * プレイヤーログイン抑止処理
     * プレイヤーのログイン拒否を含むため、まず優先度LOWSTで抑止処理をしておく
     * @param event イベント
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void RejectPlayer(PlayerLoginEvent event) {
        // プレイヤーデータ保存未使用なら何もせず終わる
        if (!efconf.dbuse) return;
        // DB接続できていない状況でまだ動作中なら何もしないで終わる
        if (efconf.store == null) return;

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
        if (!loadPlayerData(event.getPlayer().getUniqueId(), OPE.ISLOGOUT)) {
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
        // プレイヤーデータ保存未使用なら何もせず終わる
        if (!efconf.dbuse) return;
        // DB接続できていない状況でまだ動作中なら何もしないで終わる
        if (efconf.store == null) return;

        // 他のプライオリティの処理でログインが抑止されている場合何もしない
        if (!event.getResult().equals(PlayerLoginEvent.Result.ALLOWED)) {
            return;
        }
        
        // ログイン許可されている場合はDBからプレイヤーデータのロードを行う
        // 取り出しは同期処理で
        log.info("start PlayerData load.");
        
        // データベースからの取り出し
        loadPlayerData(event.getPlayer().getUniqueId(), OPE.LOAD);
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
    private boolean loadPlayerData(UUID uuid, OPE ope) {
        boolean result = true;
        String w = plg.getServer().getWorlds().get(0).getName();
        String uid = uuid.toString();
        PlayerFileSet files = new PlayerFileSet(
            new File(w + "/playerdata/" + uid + ".dat"),
            new File(w + "/stats/" + uid + ".json"),
            new File(w + "/advancements/" + uid + ".json"));
        PlayerFileStore store = efconf.store;
        Connection con = null;
        try {
            con = store.connect();
            if (store.existPlayerData(con, uuid)) {
                switch (ope) {
                    case LOAD:
                        store.loadPlayerData(con, uuid, files);
                        break;
                    case START:
                        store.logoutPlayer(con, uuid);
                        break;
                    case ISLOGOUT:
                        result = store.isPlayerAfterLogout(con, uuid);
                        break;
                }
            } else {
                log.log(Level.INFO, "not stored PlayerData:{0}", uuid);
            }
            con.commit();
        } catch (SQLException | IOException ex) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex1) {
                    Logger.getLogger(PlayerDataFileStoreListener.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
            Logger.getLogger(PlayerDataFileStoreListener.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (con != null) {
            try {
                con.close();
            } catch (SQLException ex) {
                Logger.getLogger(PlayerDataFileStoreListener.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return result;
    }
}

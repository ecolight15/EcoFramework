
package jp.minecraftuser.ecoframework.store;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import jp.minecraftuser.ecoframework.async.*;
import jp.minecraftuser.ecoframework.PluginFrame;
import static jp.minecraftuser.ecoframework.Utl.sendPluginMessage;
import jp.minecraftuser.ecoframework.exception.DatabaseControlException;

/**
 * 非同期プレイヤーデータ保存クラス
 * @author ecolight
 */
public class PlayerDataFileStoreAsyncThread extends AsyncProcessFrame {
    // 呼び出し元のリスナを覚えておく(executeReceiveのみR/W可とする)
    private PlayerFileStore store;
    private final PlayerDataFileStoreListener listener;
    private String db;
    private String dbname;
    private String dbserver;
    private String dbuser;
    private String dbpass;

    /**
     * 親スレッド用コンストラクタ
     * @param plg_ プラグインフレームインスタンス
     * @param listener_ 呼び出し元リスナーフレームインスタンス
     * @param name_ 名前
     */
    public PlayerDataFileStoreAsyncThread(PluginFrame plg_, PlayerDataFileStoreListener listener_, String name_) {
        super(plg_, name_);
        listener = listener_;
        // リロード対応
        conf.registerNotifiable(this);
        reloadNotify();
    }

    /**
     * 子スレッド用コンストラクタ
     * @param plg_ プラグインフレームインスタンス
     * @param listener 呼び出し元リスナーフレームインスタンス
     * @param name_ 名前
     * @param frame_ 子スレッド用フレーム
     */
    public PlayerDataFileStoreAsyncThread(PluginFrame plg_, PlayerDataFileStoreListener listener, String name_, AsyncFrame frame_) {
        super(plg_, name_, frame_);
        this.listener = listener;
        // リロード対応
        conf.registerNotifiable(this);
        reloadNotify();
    }

    /**
     * リロード通知受信
     */
    @Override
    public void reloadNotify() {
        PlayerFileStore buf;

        // コンフィグが情報更新しているので読み直す
        String db_ = conf.getString("userdatadb.db");
        String dbname_ = conf.getString("userdatadb.name");
        String dbserver_ = conf.getString("userdatadb.server");
        String dbuser_ = conf.getString("userdatadb.user");
        String dbpass_ = conf.getString("userdatadb.pass");

        try {
            if (db.equalsIgnoreCase("sqlite")) {
                buf = new PlayerFileStore(plg, dbname, "playerdata");
            } else if (db.equalsIgnoreCase("mysql")) {
                buf = new PlayerFileStore(plg, dbserver, dbuser, dbpass, dbname, "playerdata");
            } else {
                throw new DatabaseControlException("データベース指定が異常です");
            }
            store.close();
            db = db_;
            dbname = dbname_;
            dbserver = dbserver_;
            dbuser = dbuser_;
            dbpass = dbpass_;
            store = buf;
        } catch (Exception e) {
            log.warning(e.getLocalizedMessage());
            log.warning("データベースの再読込に失敗したため、変更前のデータベース設定で動作します");
        }
    }

    @Override
    protected void executeProcess(PayloadFrame data_) {
        PlayerDataFileStorePayload data = (PlayerDataFileStorePayload) data_;
        // ファイルの更新監視
        String w = data.getWorld();
        String uid = data.getUUID().toString();
        PlayerFileSet files = new PlayerFileSet(
            new File(w + "/playerdata/" + uid + ".dat"),
            new File(w + "/stats/" + uid + ".json"),
            new File(w + "/advancements/" + uid + ".json"));
        
        // ファイルの更新確認完了
        log.log(Level.INFO, "{0}/{1}", new Object[]{files.profile.getAbsolutePath(), files.profile.toString()});
        log.log(Level.INFO, "{0}/{1}", new Object[]{files.stats.getAbsolutePath(), files.stats.toString()});
        log.log(Level.INFO, "{0}/{1}", new Object[]{files.adv.getAbsolutePath(), files.adv.toString()});
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        log.info("開始値:" + data.getStartTime() + " profile チェック値:" + files.profile.lastModified());
        log.info("開始値:" + data.getStartTime() + " stats チェック値:" + files.stats.lastModified());
        log.info("開始値:" + data.getStartTime() + " advancements チェック値:" + files.adv.lastModified());
        log.info("開始時刻:"+sdf.format(data.getStartTime()));
        log.info("チェック時刻:" + sdf.format(files.profile.lastModified()));
        log.info("チェック時刻:" + sdf.format(files.stats.lastModified()));
        log.info("チェック時刻:" + sdf.format(files.adv.lastModified()));
        while (true) {
            long mod1 = files.profile.lastModified();
            long mod2 = files.stats.lastModified();
            long mod3 = files.adv.lastModified();
            if ((mod1 > data.getStartTime()) &&
                (mod2 > data.getStartTime()) &&
                (mod3 > data.getStartTime())) break;
            Date now = new Date();
            // 現時刻と保存日時が+-1秒以内なら完了済みとみなす
            if ((Math.abs(now.getTime() - mod1) < 1000) &&
                (Math.abs(now.getTime() - mod2) < 1000) &&
                (Math.abs(now.getTime() - mod3) < 1000)) break;
            // 保存開始から50秒経過でタイムアウト
            if (now.getTime() - data.getStartTime() > 50000) {
                sendPluginMessage(plg, null, "ユーザーデータの保存に失敗しました。本メッセージをサーバー管理者に通知してください。Player={0}, Time:{1}",
                        data.getUUID().toString(), sdf.format(now));
                break;
            }
            log.info("チェック：profile ファイル更新時刻:" + sdf.format(mod1));
            log.info("チェック：stats ファイル更新時刻:" + sdf.format(mod2));
            log.info("チェック：advancements ファイル更新時刻:" + sdf.format(mod3));
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Logger.getLogger(PlayerDataFileStoreAsyncThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        // データベースへの格納
        Connection con = null;
        try {
            con = store.connect();
            store.savePlayerData(con, data.getUUID(), files);
            con.commit();
            data.setResult(true);
        } catch (SQLException | IOException ex) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex1) {
                    Logger.getLogger(PlayerDataFileStoreAsyncThread.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
            Logger.getLogger(PlayerDataFileStoreAsyncThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (con != null) {
            try {
                con.close();
            } catch (SQLException ex) {
                Logger.getLogger(PlayerDataFileStoreAsyncThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        // 処理結果を返送
        receiveData(data);
    }

    @Override
    protected void executeReceive(PayloadFrame data_) {
        // 処理結果を受け取ったので完了する
        listener.complete(((PlayerDataFileStorePayload)data_).getUUID());
    }

    @Override
    protected AsyncFrame clone() {
        return new PlayerDataFileStoreAsyncThread(plg, listener, name, this);
    }
  
}

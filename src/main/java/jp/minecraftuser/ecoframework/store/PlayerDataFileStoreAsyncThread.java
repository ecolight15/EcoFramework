
package jp.minecraftuser.ecoframework.store;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
        // コンフィグが情報更新しているので読み直す
        db = conf.getString("userdatadb.db");
        dbname = conf.getString("userdatadb.name");
        dbserver = conf.getString("userdatadb.server");
        dbuser = conf.getString("userdatadb.user");
        dbpass = conf.getString("userdatadb.pass");
    }

    @Override
    protected void executeProcess(PayloadFrame data_) {
        PlayerDataFileStorePayload data = (PlayerDataFileStorePayload) data_;
        // ファイルの更新監視
        File datafile = new File(data.getWorld() + "/playerdata/" + data.getUUID().toString() + ".dat");
        File statfile = new File(data.getWorld() + "/stats/" + data.getUUID().toString() + ".json");
        
        // ファイルの更新確認完了
        log.info(datafile.getAbsolutePath());
        log.info(datafile.toString());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        log.info("開始値:" + data.getStartTime() + " チェック値:" + datafile.lastModified());
        log.info("開始時刻:"+sdf.format(data.getStartTime()));
        log.info("チェック時刻:" + sdf.format(datafile.lastModified()));
        while (true) {
            long mod = datafile.lastModified();
            if (mod > data.getStartTime()) break;
            Date now = new Date();
            // 現時刻と保存日時が+-1秒以内なら完了済みとみなす
            if (Math.abs(now.getTime() - mod) < 1000) break;
            // 保存開始から50秒経過でタイムアウト
            if (now.getTime() - data.getStartTime() > 50000) {
                sendPluginMessage(plg, null, "ユーザーデータの保存に失敗しました。本メッセージをサーバー管理者に通知してください。Player={0}, Time:{1}",
                        data.getUUID().toString(), sdf.format(now));
                break;
            }
            log.info("チェック：ファイル更新時刻:" + sdf.format(mod));
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Logger.getLogger(PlayerDataFileStoreAsyncThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        // データベースへの格納
        PlayerFileStore store = null;
        try {
            if (db.equalsIgnoreCase("sqlite")) {
                store = new PlayerFileStore(plg, dbname, "playerdata");
            } else if (db.equalsIgnoreCase("mysql")) {
                store = new PlayerFileStore(plg, dbserver, dbuser, dbpass, dbname, "playerdata");
            } else {
                throw new DatabaseControlException("データベース指定が異常です");
            }
//            if (store.existPlayerData(data.getUUID())) {
//                store.updatePlayerData(data.getUUID(), datafile);
//            } else {
                store.savePlayerData(data.getUUID(), datafile);
//            }
            store.commit();
            data.setResult(true);
        } catch (ClassNotFoundException | SQLException | IOException ex) {
            if (store != null) {
                try {
                    store.rollback();
                } catch (SQLException ex1) {
                    Logger.getLogger(PlayerDataFileStoreAsyncThread.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
            Logger.getLogger(PlayerDataFileStoreAsyncThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (DatabaseControlException ex) {
            Logger.getLogger(PlayerDataFileStoreAsyncThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (store != null) {
            store.close();
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

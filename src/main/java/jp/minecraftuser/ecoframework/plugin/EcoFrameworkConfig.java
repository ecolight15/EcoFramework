package jp.minecraftuser.ecoframework.plugin;

import jp.minecraftuser.ecoframework.ConfigFrame;
import jp.minecraftuser.ecoframework.PluginFrame;
import jp.minecraftuser.ecoframework.exception.DatabaseControlException;
import jp.minecraftuser.ecoframework.store.PlayerFileStore;

/**
 * EcoFramework用デフォルトコンフィグ
 * @author ecolight
 */
public class EcoFrameworkConfig extends ConfigFrame {
    public PlayerFileStore store;
    public boolean dbuse;
    /**
     * コンストラクタ
     * @param plg_ EcoFramework統合管理インスタンス
     */
    public EcoFrameworkConfig(PluginFrame plg_) {
        super(plg_);
    }

    /**
     * リロード通知受信
     * @param base
     */
    @Override
    public void reloadNotify(boolean base) {
        if (base) {
            super.reload();
        } else {
            // コンフィグが情報更新しているので読み直す
            log.info("start execute default config.");
            dbuse = conf.getBoolean("userdatadb.use");
            if (dbuse) {
                log.info("start loading player database.");
                PlayerFileStore buf;
                String db_ = conf.getString("userdatadb.db");
                String dbname_ = conf.getString("userdatadb.name");
                String dbserver_ = conf.getString("userdatadb.server");
                String dbuser_ = conf.getString("userdatadb.user");
                String dbpass_ = conf.getString("userdatadb.pass");

                try {
                    if (db_.equalsIgnoreCase("sqlite")) {
                        buf = new PlayerFileStore(plg, dbname_, "playerdata");
                    } else if (db_.equalsIgnoreCase("mysql")) {
                        buf = new PlayerFileStore(plg, dbserver_, dbuser_, dbpass_, dbname_, "playerdata");
                    } else {
                        throw new DatabaseControlException("データベース指定が異常です");
                    }
                    if (store != null) {
                        store.close();
                    }
                    store = buf;
                } catch (Exception e) {
                    log.warning(e.getLocalizedMessage());
                    e.printStackTrace();
                    log.warning("データベースの再読込に失敗したため、変更前のデータベース設定で動作します");
                }
            }
            log.info("end loading player database.");
        }
    }

}

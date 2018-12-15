package jp.minecraftuser.ecoframework.plugin;

import jp.minecraftuser.ecoframework.CommandFrame;
import jp.minecraftuser.ecoframework.ConfigFrame;
import jp.minecraftuser.ecoframework.ListenerFrame;
import jp.minecraftuser.ecoframework.PluginFrame;
import jp.minecraftuser.ecoframework.store.PlayerDataFileStoreListener;

public class EcoFramework extends PluginFrame {

    /**
     * プラグイン開始処理
     */
    @Override
    public void onEnable() {
        initialize();
        log.info("本フレームワーク使用プラグインは plugin.yml に depend: [EcoFramework] 指定すること");
    }

    /**
     * プラグイン終了処理
     */
    @Override
    public void onDisable()
    {
        // DB動作していたら止める
        ListenerFrame f = getPluginListerner("playerdata");
        if (f != null) {
            ((PlayerDataFileStoreListener) f).close();
        }
        disable();
    }

    /**
     * コンフィグ初期化処理
     */
    @Override
    protected void initializeConfig() {
        // 他のEcoFrameworkプラグイン自体の単体再起動を可能にするため設定ファイルを読み込む
        ConfigFrame conf = new EcoFrameworkConfig(this);
        conf.registerBoolean("userdatadb.use");
        conf.registerString("userdatadb.db");
        conf.registerString("userdatadb.name");
        conf.registerString("userdatadb.server");
        conf.registerString("userdatadb.user");
        conf.registerString("userdatadb.pass");
        registerPluginConfig(conf);
    }

    /**
     * リスナー初期化処理
     */
    @Override
    protected void initializeListener() {
        ConfigFrame conf = getDefaultConfig();
        if (conf.getBoolean("userdatadb.use")) {
            registerPluginListener(new PlayerDataFileStoreListener(this, "playerdata"));
        }
    }

    /**
     * コマンド初期化
     */
    @Override
    public void initializeCommand() {
        // EcoFramework本体コマンド
        CommandFrame cmd = new EcoFrameworkCommand(this, "ecoframework");
        cmd.addCommand(new EcoFrameworkPermissionsCommand(this, "permissions"));
        registerPluginCommand(cmd);
    }
}

package jp.minecraftuser.ecoframework;

import java.util.logging.Level;
import java.util.logging.Logger;
import jp.minecraftuser.ecoframework.iface.Manageable;
import jp.minecraftuser.ecoframework.iface.ReloadNotifiable;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * タイマー共通フレームワーククラス
 * @author ecolight
 */
public abstract class TimerFrame extends BukkitRunnable implements ReloadNotifiable, Manageable {
    protected final String name;
    protected final Logger log;
    protected final PluginFrame plg;
    protected final ConfigFrame conf;
    protected final Player player;
    protected ManagerFrame manager = null;

    /**
     * コンストラクタ
     * @param plg_ プラグインインスタンス
     * @param name_ 名前
     */
    public TimerFrame(PluginFrame plg_, String name_) {
        plg = plg_;
        name = name_;
        log = plg.getLogger();
        conf = plg.getDefaultConfig();
        manager = plg.getManager();
        player = null;
    }

    /**
     * コンストラクタ
     * @param plg_ プラグインインスタンス
     * @param name_ 名前
     * @param player_ プレイヤーインスタンス
     */
    public TimerFrame(PluginFrame plg_, Player player_, String name_) {
        plg = plg_;
        name = name_;
        log = plg.getLogger();
        conf = plg.getDefaultConfig();
        manager = plg.getManager();
        player = player_;
    }

    /**
     * 名前取得
     * @return 名前
     */
    public String getName() {
        return name;
    }

    /**
     * マネージャー登録処理
     * @param manager_ マネージャーフレームインスタンス
     */
    @Override
    public void registerManager(ManagerFrame manager_) {
        manager = manager_;
    }

}

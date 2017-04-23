
package jp.minecraftuser.ecoframework;

import java.util.logging.Logger;
import jp.minecraftuser.ecoframework.iface.Manageable;
import jp.minecraftuser.ecoframework.iface.ReloadNotifiable;
import org.bukkit.event.Listener;

/**
 * リスナー共通フレームワーククラス
 * @author ecolight
 */
public abstract class ListenerFrame implements Listener, ReloadNotifiable, Manageable {
    protected Logger log;
    protected final PluginFrame plg;
    protected final ConfigFrame conf;
    protected final String name;
    protected ManagerFrame manager = null;

    /**
     * コンストラクタ
     * @param plg_ プラグインインスタンス
     */
    public ListenerFrame(PluginFrame plg_, String name_) {
        plg = plg_;
        log = plg.getLogger();
        conf = plg.getDefaultConfig();
        name = name_;
        manager = plg.getManager();
        plg.getServer().getPluginManager().registerEvents(this, plg);
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


package jp.minecraftuser.ecoframework;

import java.util.logging.Logger;
import jp.minecraftuser.ecoframework.iface.ReloadNotifiable;

/**
 * マネージャークラス
 * 本クラスを継承したクラスをPluginFrameに登録しておくと終了時にcloseがコールされる。
 * プラグイン停止時に終息処理が必要な場合は、リソースの保持を含め本クラスが担当する。
 * @author ecolight
 */
public abstract class ManagerFrame implements ReloadNotifiable {
    protected final Logger log;
    protected final PluginFrame plg;
    protected final ConfigFrame conf;

    /**
     * コンストラクタ
     * @param plg_ プラグインインスタンス
     */
    public ManagerFrame(PluginFrame plg_) {
        plg = plg_;
        log = plg.getLogger();
        conf = plg.getDefaultConfig();
    }
    
    /**
     * 終了関数
     */
    protected abstract void close();
}

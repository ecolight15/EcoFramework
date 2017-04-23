
package jp.minecraftuser.ecoframework.iface;

import jp.minecraftuser.ecoframework.ManagerFrame;

/**
 * PluginFrameへのマネージャー登録時にインスタンスを受け取る事を示す
 * @author ecolight
 */
public interface Manageable {

    /**
     * ManagerFrameインスタンス受け取り処理を実装すること
     * @param manager_ マネージャーフレームインスタンス
     */
    public abstract void registerManager(ManagerFrame manager_);
}

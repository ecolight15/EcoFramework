
package jp.minecraftuser.ecoframework.store;

import java.util.Date;
import jp.minecraftuser.ecoframework.async.*;
import java.util.UUID;
import jp.minecraftuser.ecoframework.PluginFrame;

/**
 * メインスレッドと非同期スレッド間のデータ送受用クラス(メッセージ送受用)
 * @author ecolight
 */
public class PlayerDataFileStorePayload extends PayloadFrame {
    public UUID uuid;
    public String world;
    public long datetime;
    public boolean result = false;

    /**
     * コンストラクタ
     * @param plg_ プラグインインスタンス(ただし通信に用いられる可能性を念頭に一定以上の情報は保持しない)
     */
    public PlayerDataFileStorePayload(PluginFrame plg_, UUID uuid_) {
        super(plg_);
        uuid = uuid_;
        datetime = new Date().getTime();
        // サーバーのプレイヤーデータ格納先を確保
        // (getWorldsの0番目がserver.propertiesのlevel-nameのワールド)
        world = plg_.getServer().getWorlds().get(0).getName();
    }
}

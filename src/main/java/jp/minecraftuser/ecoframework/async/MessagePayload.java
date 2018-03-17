
package jp.minecraftuser.ecoframework.async;

import java.util.UUID;
import jp.minecraftuser.ecoframework.PluginFrame;

/**
 * メインスレッドと非同期スレッド間のデータ送受用クラス(メッセージ送受用)
 * @author ecolight
 */
public class MessagePayload extends PayloadFrame {
    private UUID uuid;
    private UUID[] uuids;
    private String msg;

    /**
     * コンストラクタ
     * @param plg_ プラグインインスタンス(ただし通信に用いられる可能性を念頭に一定以上の情報は保持しない)
     */
    public MessagePayload(PluginFrame plg_, UUID uuid_, UUID[] uuids_, String msg_) {
        super(plg_);
        uuid = uuid_;
        uuids = uuids_;
        msg = msg_;
    }
    
    /**
     * UUID返却
     * @return UUID
     */
    public UUID getUUID() {
        return uuid;
    }
    
    /**
     * UUID配列返却
     * @return UUID配列
     */
    public UUID[] getUUIDs() {
        return uuids;
    }
    
    public String getMessage() {
        return msg;
    }
}


package jp.minecraftuser.ecoframework.async;

import jp.minecraftuser.ecoframework.PluginFrame;
import org.bukkit.command.CommandSender;

/**
 * メインスレッドと非同期スレッド間のデータ送受用クラス(メッセージ送受用)
 * @author ecolight
 */
public class MessagePayload extends PayloadFrame {
    private CommandSender sender;
    private CommandSender target;
    private String msg;

    /**
     * コンストラクタ
     * @param plg_ プラグインインスタンス(ただし通信に用いられる可能性を念頭に一定以上の情報は保持しない)
     * @param sender_ 送信者
     * @param target_ 送信先
     * @param msg_ 送信メッセージ
     */
    public MessagePayload(PluginFrame plg_, CommandSender sender_, CommandSender target_, String msg_) {
        super(plg_);
        sender = sender_;
        target = target_;
        msg = msg_;
    }
    
    /**
     * sender取得
     * @return CommandSender 送信者
     */
    public CommandSender getSender() {
        return sender;
    }
    
    /**
     * target取得
     * @return CommandSender 送信先
     */
    public CommandSender getTarget() {
        return target;
    }
    
    /**
     * 送信メッセージ取得
     * @return String 送信メッセージ
     */
    public String getMessage() {
        return msg;
    }
}


package jp.minecraftuser.ecoframework.plugin;

import jp.minecraftuser.ecoframework.PluginFrame;
import jp.minecraftuser.ecoframework.CommandFrame;
import org.bukkit.command.CommandSender;

/**
 * Confirmキャンセルコマンドクラス
 * @author ecolight
 */
public class EcoFrameworkCancelCommand extends CommandFrame {

    /**
     * コンストラクタ
     * @param plg_ プラグインインスタンス
     * @param name_ コマンド名
     */
    public EcoFrameworkCancelCommand(PluginFrame plg_, String name_) {
        super(plg_, name_);
        setAuthBlock(false);
        setAuthConsole(false);
    }

    /**
     * コマンド権限文字列設定
     * @return 権限文字列
     */
    @Override
    public String getPermissionString() {
        return "ecoframework.cancel";
    }

    /**
     * 処理実行部
     * @param sender コマンド送信者
     * @param args パラメタ
     * @return コマンド処理成否
     */
    @Override
    public boolean worker(CommandSender sender, String[] args) {
        // パラメータチェック:0のみ
        if (!checkRange(sender, args, 0, 0)) return true;

        // cancel呼び出しで該当コマンドのコールバック呼び出しさせる
        plg.cancel_confirm(sender);
        return true;
    }
}

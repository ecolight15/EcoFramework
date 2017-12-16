
package jp.minecraftuser.ecoframework.plugin;

import jp.minecraftuser.ecoframework.CommandFrame;
import jp.minecraftuser.ecoframework.PluginFrame;
import jp.minecraftuser.ecoframework.Utl;
import org.bukkit.command.CommandSender;

/**
 * パーミッションリスト取得コマンドクラス
 * @author ecolight
 */
public class EcoFrameworkPermissionsCommand extends CommandFrame {

    /**
     * コンストラクタ
     * @param plg_ プラグインインスタンス
     * @param name_ コマンド名
     */
    public EcoFrameworkPermissionsCommand(PluginFrame plg_, String name_) {
        super(plg_, name_);
        setAuthBlock(true);
        setAuthConsole(true);
    }

    /**
     * コマンド権限文字列設定
     * @return 権限文字列
     */
    @Override
    public String getPermissionString() {
        return "ecoframework.permissions";
    }

    /**
     * 処理実行部
     * @param sender コマンド送信者
     * @param args パラメタ
     * @return コマンド処理成否
     */
    @Override
    public boolean worker(CommandSender sender, String[] args) {
        // パラメータチェック:1つのみ
        if (!checkRange(sender, args, 0, 1)) return true;

        // プラグイン権限リスト表示
        Utl.sendPluginMessage(plg, sender, "プラグイン権限リストここから");
        for (PluginFrame p : plg.getRefPluginList()) {
            // プラグイン指定の場合、対象以外はスキップ
            if ((args.length == 1) && (!p.getName().equalsIgnoreCase(args[0]))) continue;
            
            // 表示する
            Utl.sendPluginMessage(plg, sender, "plugin[{0}]", p.getName());
            for (String s : p.getPermissionList()) {
                Utl.sendPluginMessage(plg, sender, "  - {0}", s);
            }

            // プラグイン指定でここまで到達した場合、目的のプラグインの出力は終わってるので抜ける
            if ((args.length == 1) && (p.getName().equalsIgnoreCase(args[0]))) break;
        }
        Utl.sendPluginMessage(plg, sender, "プラグイン権限リストここまで");
        return true;
    }
    
}

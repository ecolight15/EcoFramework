
package jp.minecraftuser.ecoframework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import static jp.minecraftuser.ecoframework.Utl.sendPluginMessage;
import jp.minecraftuser.ecoframework.iface.Manageable;
import jp.minecraftuser.ecoframework.iface.ReloadNotifiable;
import jp.minecraftuser.ecoframework.plugin.EcoFramework;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

/**
 * コマンド共通フレームワーククラス
 * @author ecolight
 */
public abstract class CommandFrame implements ReloadNotifiable, Manageable {
    protected final Logger log;
    protected final PluginFrame plg;
    protected final String name;
    protected final HashMap<String, CommandFrame> cmds;
    protected final ConfigFrame conf;
    protected ManagerFrame manager = null;
    protected boolean blockPermission = false;
    protected boolean consolePermission = false;
    protected boolean playerPermission = true;
    
    /**
     * コンストラクタ
     * @param plg_ プラグインインスタンス
     * @param name_ コマンド名
     */
    public CommandFrame(PluginFrame plg_, String name_) {
        plg = plg_;
        name = name_.toLowerCase();
        log = plg.getLogger();
        conf = plg.getDefaultConfig();
        cmds = new HashMap<>();
        manager = plg.getManager();
    }

    /**
     * マネージャー登録処理
     * @param manager_ マネージャーフレームインスタンス
     */
    @Override
    public void registerManager(ManagerFrame manager_) {
        manager = manager_;
    }

    /**
     * サブコマンド登録処理
     * @param cmd_ サブコマンドインスタンス
     */
    public void addCommand(CommandFrame cmd_) {
        cmds.put(cmd_.getName(), cmd_);
    }

    /**
     * サブコマンド一覧取得処理
     * @return サブコマンド一覧
     */
    public Collection<CommandFrame> getCommandList() {
        return cmds.values();
    }

    /**
     * サブコマンド取得処理
     * @param name_ サブコマンド名
     * @return サブコマンド
     */
    public CommandFrame getCommand(String name_) {
        return cmds.get(name_);
    }

    /**
     * コマンド起動
     * @param sender コマンド送信者
     * @param args パラメタ
     * @return コマンド成否
     */
    public boolean execute(CommandSender sender, String[] args) {

        // サブコマンド登録がある場合、該当コマンドへ実行を引き継ぐ
        if ((!cmds.isEmpty()) &&
            (args.length >= 1)) {
            if (cmds.containsKey(args[0])) {
                ArrayList<String> new_args = new ArrayList();
                // パラメタ配列から移譲コマンドのパラメタを削り取る
                Boolean first = true;
                for (String s: args) {
                    if (first) {
                        first = false;
                        continue;
                    }
                    new_args.add(s);
                }
                return cmds.get(args[0]).execute(sender, new_args.toArray(new String[0]));
            } 
        }
        
        // consoleが実行可能かチェックする
        if ((sender instanceof ConsoleCommandSender) && (!consolePermission)) {
            sendPluginMessage(plg, sender, "コンソールから使用できません。");
            return true;
        }
        
        // コマンドブロックが実行可能かチェックする
        if ((sender instanceof BlockCommandSender) && (!blockPermission)) {
            sendPluginMessage(plg, sender, "コマンドブロックから使用できません。");
            return true;
        }
        
        // プレイヤーの場合、プレイヤー情報を元に権限を判定する
        if (sender instanceof Player) {
            if ((sender instanceof Player) && (!playerPermission)) {
                sendPluginMessage(plg, sender, "プレイヤーは使用できません。(権限設定を見直してください)");
                return true;
            }
            if (!((Player)sender).hasPermission(getPermissionString())) {
                sendPluginMessage(plg, sender, "コマンドの使用権限がありません。");
                return true;
            }
        }
        return worker(sender, args);
    }

    /**
     * コマンド名返却
     * @return コマンド名
     */
    public String getName() {
        return name;
    }
    
    /**
     * タブコンプリート処理
     * @param sender コマンド送信者インスタンス
     * @param cmd コマンドインスタンス
     * @param string コマンド文字列
     * @param strings パラメタ文字列配列
     * @return 保管文字列配列
     */
    protected List<String> onTabComplete(CommandSender sender, Command cmd, String string, String[] strings) {
        List<String> result = new ArrayList<>();
        // パラメタが2以上かつ1つ目のパラメタが管理下のサブコマンドだったら処理を移譲する
        if ((!cmds.isEmpty()) &&
            (strings.length >= 2)) {
            String check = strings[0].toLowerCase();
            if (check.startsWith(plg.getName().toLowerCase() + ":")) {
                check = check.substring(check.lastIndexOf(":") + 1);
            }
            if (cmds.containsKey(check)) {
                ArrayList<String> new_args = new ArrayList();
                // パラメタ配列から移譲コマンドのパラメタを削り取る
                Boolean first = true;
                for (String s: strings) {
                    if (first) {
                        first = false;
                        continue;
                    }
                    new_args.add(s);
                }
                return cmds.get(strings[0].toLowerCase()).onTabComplete(sender, cmd, strings[0], new_args.toArray(new String[0]));
            } 
        }
        // 上の処理に引っかからなければこのコマンドで処理する
        // 各バリエーションについて考えていく
        
        // まず使用権限チェック
        // 権限がない場合には候補を返さない
        // TAB送り付けてくるのはクライアントだけなのでプレイヤーチェックのみ
        if (!playerPermission) {
            return result;
        }
        if (!sender.hasPermission(getPermissionString())) {
            return result;
        }

        // 入力中(パラメタが1つ)の場合
        // - 候補としてサブコマンドのケース
        //   - サブコマンドマップのキーを前方一致検索して合致するものを返す
        // - 候補として独自パラメタのケース
        //   - コマンド毎の独自パラメタ候補を返却する(要継承先実装)
        for (String s : cmds.keySet()) {
            if (s.startsWith(strings[0].toLowerCase())) {
                result.add(s);
            }
        }

        // 実装にもArrayListを返させてマージする
        // 実装が返さない場合、かつサブコマンドが候補に無い場合にはデフォルトでPlayerリスト(前方一致)を返却する
        List<String> list = getTabComplete(sender, cmd, string, strings);
        // 何も候補がない場合プレイヤー一覧を返していたが、プレイヤーを指定する場面でないのに表示されると混乱をきたすため空のままにする
//        if (((list == null) || (list.isEmpty())) && (result.isEmpty())) {
//            list = new ArrayList<>();
//            for (Player p : plg.getServer().getOnlinePlayers()) {
//                if (p.getName().toLowerCase().startsWith(strings[0].toLowerCase())) {
//                    list.add(p.getName());
//                }
//            }
//        }
        // マージ
        if (list != null) {
            for (String s : list) {
                result.add(s);
            }
        }

        // パラメタが2個以上の場合：1個目のパラメタが実はサブコマンドのつもり
        // - 誤りなので何もしないかその旨伝えるべき？(要継承先実装:独自パラメタの期待値でないことの確認が必要)
        
        // コマンドの実装で2つ以上の独自パラメタを要求している場合
        // - 入力状況としては正しい(要継承先実装:独自パラメタの期待値であることの確認が必要)
        // - 正しいと認識できるパラメタの後に続くパラメタが存在する場合、コマンド毎の独自のパラメタ候補を返却する
        
        // パラメタが2以上の場合は、全部 getTabComplete で判別すること

        // List<String>を返却するとその順次表示、nullを返すとPlayerの順次表示が行われる
        // なおListに「パラメタ候補+ブランク+次パラメタ」で文字列を設定すると「パラメタ候補」は決定パラメタとして認識される
        // newだけした空のListを返すと候補なしとなりTABを続けて押しても何も起きなくなる
        return result;
    }

    /**
     * コマンド別タブコンプリート処理
     * @param sender コマンド送信者インスタンス
     * @param cmd コマンドインスタンス
     * @param string コマンド文字列
     * @param strings パラメタ文字列配列
     * @return 保管文字列配列
     */
    protected List<String> getTabComplete(CommandSender sender, Command cmd, String string, String[] strings) {
        // optional method
        return null;
    }

    /**
     * パラメタチェック処理(定型メッセージを送信者に返却する)
     * 定型処理として組み込むことも検討したが、コマンドによってはパラメタによって
     * 追加でパラメタチェックする事もあると思うので任意に呼べるメソッドとして提供する。
     * @param sender コマンド送信者インスタンス
     * @param args パラメタ分割文字列
     * @param min 許容する最小パラメタ数、負数の場合にはチェックしない
     * @param max 許容する最大パラメタ数、負数の場合にはチェックしない
     * @return コマンドパラメタ数異常かどうかのbool値を返す
     */
    protected final boolean checkRange(CommandSender sender, String[] args, int min, int max) {
        if ((min >= 0) && (args.length < min)) {
            sendPluginMessage(plg, sender, "指定したコマンドパラメーターが不足しています。");
            return false;
        } else if ((max >= 0) && (args.length > max)) {
            sendPluginMessage(plg, sender, "指定したコマンドパラメーターが多すぎます。");
            return false;
        }
        return true;
    }

    /**
     * 権限リスト表示
     * @return 権限文字列リスト
     */
    public ArrayList<String> getPermissionList(ArrayList<String> list) {
        // パラメタが空であればルートなので生成して下位に渡す
        if (list == null) {
            list = new ArrayList<>();
        }
        
        // まず自分のコマンドの権限を挿入する
        list.add(getPermissionString());

        // サブコマンド登録がある場合、該当コマンドへ実行を引き継ぐ
        for (CommandFrame c : cmds.values()) {
            c.getPermissionList(list);
        }
        return list;
    }
    
    /**
     * コンソールユーザー実行可否設定
     * デフォルト実行不可
     * @param perm 実行可否
     */
    protected final void setAuthConsole(boolean perm) {
        consolePermission = perm;
    }
    
    /**
     * ブロック実行可否設定
     * デフォルト実行不可
     * @param perm 実行可否
     */
    protected final void setAuthBlock(boolean perm) {
        blockPermission = perm;
    }
    
    /**
     * プレイヤー実行可否設定
     * デフォルト実行可にしておく
     * @param perm 実行可否
     */
    protected final void setAuthPlayer(boolean perm) {
        playerPermission = perm;
    }
    
    /**
     * 確認待ちの登録
     * @param sender 
     */
    protected void confirm(CommandSender sender) {
        // 既にconfirm待機中の場合、キャンセルが動作するので実行確認メッセージの前にstart_confirmしておく
        // confirm/cancel コマンドは一括でEcoFrameworkプラグインに処理させるため、EcoFrameworkプラグインにconfirm開始させる
        ((EcoFramework)plg.getServer().getPluginManager().getPlugin("EcoFramework")).start_confirm(sender, this);

        // 本当にコマンドを実行しますか？[実行する][キャンセル]
        String[] ver_str = plg.getServer().getBukkitVersion().split(Pattern.quote("."));
        int[] ver = {Integer.parseInt(ver_str[0]),Integer.parseInt(ver_str[1])};
        if (ver[0] == 1 && ver[1] <= 7) {
            // 1.15 
            sendPluginMessage(plg, sender, "本当にコマンドを実行しますか？");
            sendPluginMessage(plg, sender, "実行：/ecoframework accept");
            sendPluginMessage(plg, sender, "中止：/ecoframework cancel");
        } else if (ver[0] == 1 && (ver[1] >= 8) && (ver[1] <= 15)){
            // 1.8 - 1.15 
            plg.getServer().dispatchCommand(
                plg.getServer().getConsoleSender(),
                "tellraw " + sender.getName() + " " + "[\"\",{\"text\":\"\\u672c\\u5f53\\u306b\\u30b3\\u30de\\u30f3\\u30c9\\u3092\\u5b9f\\u884c\\u3057\\u307e\\u3059\\u304b\\uff1f\\n\"},{\"text\":\"[\\u5b9f\\u884c\\u3059\\u308b]\",\"bold\":true,\"color\":\"aqua\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/ecoframework accept\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"\\u30af\\u30ea\\u30c3\\u30af\\u3067\\u30b3\\u30de\\u30f3\\u30c9\\u3092\\u5b9f\\u884c\\u3057\\u307e\\u3059\"}},{\"text\":\" / \"},{\"text\":\"[\\u30ad\\u30e3\\u30f3\\u30bb\\u30eb]\",\"bold\":true,\"color\":\"red\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/ecoframework cancel\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"\\u30af\\u30ea\\u30c3\\u30af\\u3067\\u30b3\\u30de\\u30f3\\u30c9\\u3092\\u30ad\\u30e3\\u30f3\\u30bb\\u30eb\\u3057\\u307e\\u3059\"}}]"
            );
        } else {
            // 1.16 -
            plg.getServer().dispatchCommand(
                plg.getServer().getConsoleSender(),
                "tellraw " + sender.getName() + " " + "[\"\",{\"text\":\"\\u672c\\u5f53\\u306b\\u30b3\\u30de\\u30f3\\u30c9\\u3092\\u5b9f\\u884c\\u3057\\u307e\\u3059\\u304b\\uff1f\\n\"},{\"text\":\"[\\u5b9f\\u884c\\u3059\\u308b]\",\"bold\":true,\"color\":\"aqua\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/ecoframework accept\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":\"\\u30af\\u30ea\\u30c3\\u30af\\u3067\\u30b3\\u30de\\u30f3\\u30c9\\u3092\\u5b9f\\u884c\\u3057\\u307e\\u3059\"}},{\"text\":\" / \"},{\"text\":\"[\\u30ad\\u30e3\\u30f3\\u30bb\\u30eb]\",\"bold\":true,\"color\":\"red\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/ecoframework cancel\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":\"\\u30af\\u30ea\\u30c3\\u30af\\u3067\\u30b3\\u30de\\u30f3\\u30c9\\u3092\\u30ad\\u30e3\\u30f3\\u30bb\\u30eb\\u3057\\u307e\\u3059\"}}]"
            );
        }
    }
    
    /**
     * コマンド権限文字列設定
     * @return 権限文字列
     */
    protected abstract String getPermissionString();
    
    /**
     * 処理実行部
     * @param sender コマンド送信部
     * @param args パラメタ
     * @return コマンド処理成否
     */
    protected abstract boolean worker(CommandSender sender, String[] args);

    /**
     * accept 受付用
     * @param sender 
     */
    protected void acceptCallback(CommandSender sender) {
        // optional
        log.log(Level.INFO, "callback accept not supported:{0}", name);
    }
    
    /**
     * cancel 受付用
     * @param sender 
     */
    protected void cancelCallback(CommandSender sender) {
        // optional
        log.log(Level.INFO, "callback cancel not supported:{0}", name);
    }
}

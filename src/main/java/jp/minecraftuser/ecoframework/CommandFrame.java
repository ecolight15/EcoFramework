
package jp.minecraftuser.ecoframework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import static jp.minecraftuser.ecoframework.Utl.sendPluginMessage;
import jp.minecraftuser.ecoframework.iface.Manageable;
import jp.minecraftuser.ecoframework.iface.ReloadNotifiable;
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
            if (cmds.containsKey(strings[0].toLowerCase())) {
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
        if (((list == null) || (list.isEmpty())) && (result.isEmpty())) {
            list = new ArrayList<>();
            for (Player p : plg.getServer().getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(strings[0].toLowerCase())) {
                    list.add(p.getName());
                }
            }
        }
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
    private List<String> getTabComplete(CommandSender sender, Command cmd, String string, String[] strings) {
        // optional method
        return null;
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
    public final void setAuthPlayer(boolean perm) {
        playerPermission = perm;
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
            
}

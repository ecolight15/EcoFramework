
package jp.minecraftuser.ecoframework;

import java.text.MessageFormat;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 汎用ユーティリティ
 * @author ecolight
 */
public class Utl {
    /**
     * 色置換処理
     * &記号で書かれた色指定をセクション記号に置き換える
     * 加えて全角スペースは半角スペース二つに置き換える
     * @param msg 色置換文字列
     * @return 
     */
    public static String repColor(String msg) {
        return msg.replaceAll("&([0-9A-Fa-flLmMnNoOkKrR])", "§$1").replaceAll("　", "  ");
    }

    /**
     * メッセージ送信処理(PluginPrefix)
     * @param plg プラグインインスタンス
     * @param sender 送信者インスタンス(nullの場合ブロードキャスト)
     * @param msg 送信文字列
     */
    public static void sendPluginMessage(PluginFrame plg, CommandSender sender, String msg) {
        StringBuilder sb = new StringBuilder();
        if (sender instanceof Player) {
            sb.append(ChatColor.YELLOW);
            sb.append("[");
            sb.append(plg.getName());
            sb.append("] ");
            sb.append(ChatColor.RESET);
        } else {
            sb.append("[");
            sb.append(plg.getName());
            sb.append("] ");
        }
        sb.append(msg);
        if (sender != null) {
            sender.sendMessage(sb.toString());
        } else {
            plg.getServer().broadcastMessage(sb.toString());
        }
    }


    /**
     * メッセージ送信処理(PluginPrefix)
     * @param plg プラグインインスタンス
     * @param sender 送信者インスタンス(nullの場合ブロードキャスト)
     * @param msg 送信文字列
     * @param param フォーマット文字列
     */
    public static void sendPluginMessage(PluginFrame plg, CommandSender sender, String msg, String... param) {
        MessageFormat mf = new MessageFormat(msg);
        StringBuilder sb = new StringBuilder();
        if (sender instanceof Player) {
            sb.append(ChatColor.YELLOW);
            sb.append("[");
            sb.append(plg.getName());
            sb.append("] ");
            sb.append(ChatColor.RESET);
        } else {
            sb.append("[");
            sb.append(plg.getName());
            sb.append("] ");
        }
        sb.append(mf.format(param));
        if (sender != null) {
            sender.sendMessage(sb.toString());
        } else {
            plg.getServer().broadcastMessage(sb.toString());
        }
    }

    /**
     * メッセージ送信処理(TagPrefix)
     * @param plg プラグインインスタンス
     * @param sender 送信者インスタンス(nullの場合ブロードキャスト)
     * @param tag プリフィックスタグ
     * @param msg 送信文字列
     */
    public static void sendTagMessage(PluginFrame plg, CommandSender sender, String tag,  String msg) {
        StringBuilder sb = new StringBuilder();
        if (sender instanceof Player) {
            sb.append(ChatColor.LIGHT_PURPLE);
            sb.append("[");
            sb.append(tag);
            sb.append("] ");
            sb.append(ChatColor.RESET);
        } else {
            sb.append("[");
            sb.append(plg.getName());
            sb.append("] ");
        }
        sb.append(msg);
        if (sender != null) {
            sender.sendMessage(sb.toString());
        } else {
            plg.getServer().broadcastMessage(sb.toString());
        }
    }

    /**
     * メッセージ送信処理(TagPrefix)
     * @param plg プラグインインスタンス
     * @param sender 送信者インスタンス(nullの場合ブロードキャスト)
     * @param tag プリフィックスタグ
     * @param msg 送信文字列
     * @param param フォーマット文字列
     */
    public static void sendTagMessage(PluginFrame plg, CommandSender sender, String tag, String msg, String... param) {
        MessageFormat mf = new MessageFormat(msg);
        StringBuilder sb = new StringBuilder();
        if (sender instanceof Player) {
            sb.append(ChatColor.LIGHT_PURPLE);
            sb.append("[");
            sb.append(tag);
            sb.append("] ");
            sb.append(ChatColor.RESET);
        } else {
            sb.append("[");
            sb.append(plg.getName());
            sb.append("] ");
        }
        sb.append(mf.format(param));
        if (sender != null) {
            sender.sendMessage(sb.toString());
        } else {
            plg.getServer().broadcastMessage(sb.toString());
        }
    }

    /**
     * パラメタ文字列結合
     * @param args パラメタ文字列
     * @return 結合文字列
     */
    public static String mergeStrings(String[] args) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String s : args) {
            if (first) {
                first = false;
            } else {
                sb.append(" ");
            }
            sb.append(s);
        }
        return sb.toString();
    }
}

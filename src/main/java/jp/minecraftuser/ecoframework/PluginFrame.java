
package jp.minecraftuser.ecoframework;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jp.minecraftuser.ecoframework.iface.Manageable;
import jp.minecraftuser.ecoframework.iface.ReloadNotifiable;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * プラグイン共通フレームワーククラス
 * @author ecolight
 */
public abstract class PluginFrame extends JavaPlugin {
    private final ArrayList<ReloadNotifiable> reloadList = new ArrayList<>();
    protected Logger log = null;
    protected HashMap<String, CommandFrame> cmdMap = null;
    protected HashMap<String, ListenerFrame> listenerMap = null;
    protected HashMap<String, TimerFrame> timerMap = null;
    protected HashMap<String, ConfigFrame> confMap = null;
    protected HashMap<String, DatabaseFrame> dbMap = null;
    protected HashMap<String, PluginFrame> plgMap = null;
    protected HashMap<String, PluginFrame> refMap = null;
    protected HashMap<String, JavaPlugin> otherMap = null;
    protected HashMap<String, LoggerFrame> loggerMap = null;
    protected ManagerFrame manager = null;

    /**
     * 初期化処理
     */
    protected void initialize() {
        log = getLogger();
        cmdMap = new HashMap<>();
        listenerMap = new HashMap<>();
        timerMap = new HashMap<>();
        confMap = new HashMap<>();
        dbMap = new HashMap<>();
        plgMap = new HashMap<>();
        refMap = new HashMap<>();
        otherMap = new HashMap<>();
        loggerMap = new HashMap<>();

        // 概ね読み込み順序不具合でない感じにinitialize呼んでいく
        // コンフィグ初期ロード
        initializeConfig();

        // Logger登録
        initializeLogger();

        // PlugManが存在する状況でのみsuspendしているプラグインを復旧する
        Plugin pl = Bukkit.getPluginManager().getPlugin("PlugMan");
        if (pl != null) {
            // コンフィグファイルがある場合のみ。
            // 逆説的にはプラグイン単体のリロードを可能にするには初期configファイルは必須とする。
            ConfigFrame cf = getDefaultConfig();
            if (cf != null) {
                FileConfiguration f = cf.getConf();
                List<String> str = f.getStringList("suspend");
                if (str != null) {
                    for (String s : str) {
                        getServer().dispatchCommand(getServer().getConsoleSender(), "plugman load " + s);
                    }
                    f.set("suspend", null);
                    getDefaultConfig().saveConfig();
                }
            }
        }
        
        // フレームワーク本体以外はフレームワークへの依存関係登録をする
        // これによりフレームワーク本体のリロードを可能とする
        if (!getName().equalsIgnoreCase("EcoFramework")) {
            registerPluginFrame("EcoFramework");
        }

        // DB登録
        initializeDB();
        
        // 実行コマンド登録
        initializeCommand();
        
        // イベントリスナ登録
        initializeListener();
        
        // タイマー登録
        initializeTimer();
        
        // LoggerFrame起動
        for (LoggerFrame f : loggerMap.values()) {
            f.start();
        }
        getLogger().info("plugin frame initialized.");
    }

    /**
     * 後処理
     */
    protected void disable() {
        Plugin pl = Bukkit.getPluginManager().getPlugin("PlugMan");
        // 自分を参照している相手に参照を解除させる
        for (PluginFrame plg : refMap.values().toArray(new PluginFrame[0])) {
            if (pl == null) {
                plg.unregisterPluginFrame(getName());
            } else {
                // PlugMan存在する場合コマンド経由で参照プラグインを停止する
                // 相手から自分に対する参照もこれで解除する
                // 設定ファイルにサスペンド状態を記録しておき、再起動後に起動するようにしておく
                suspendPlugin(plg);
            }
        }
        // Framework以外も同じことをする
        for (JavaPlugin plg : otherMap.values().toArray(new JavaPlugin[0])) {
            suspendPlugin(plg);
        }
        
        // 自分が参照しているプラグインを参照解除する
        for (PluginFrame plg : plgMap.values().toArray(new PluginFrame[0])) {
            unregisterPluginFrame(plg.getName());
        }
        
        // DB全部閉じる
        for (DatabaseFrame db : dbMap.values().toArray(new DatabaseFrame[0])) {
            db.close();
        }

        // タイマの全キャンセル正直多分bukkitがやってる
        // 一回CancelかけたタイマはリサイクルできないのでMapから抜いておこう
        for (TimerFrame t : timerMap.values()) {
            t.cancel();
        }
        
        // マネージャーが存在していたらcloseする
        if (manager != null) {
            manager.close();
        }

        // LoggerFrame停止
        stopAllPluginLogger();

        log.info("plugin frame ended.");
    }
    
    /**
     * 依存プラグイン登録処理
     * @param name プラグイン名
     */
    public void registerPluginFrame(String name) {
        Plugin pl = Bukkit.getPluginManager().getPlugin("PlugMan");
        Plugin dest = Bukkit.getPluginManager().getPlugin(name);
        if (dest == null) {
            // プラグイン未ロードの場合、PlugManがいる場合はコマンド経由でロードしてから処理する
            // PlugManがいない場合は初回起動の可能性があるので無理しない
            if (pl != null) {
                getServer().dispatchCommand(getServer().getConsoleSender(), "plugman load " + name);
                dest = Bukkit.getPluginManager().getPlugin(name);
            }
        }
        if (dest != null) {
            plgMap.put(name, (PluginFrame) Bukkit.getPluginManager().getPlugin(name));
            // 相手に自分が参照していることを伝える
            plgMap.get(name).addRefPluginFrame(this);
            log.info("Register plugin [" + name + "]");
        }
    }

    /**
     * Framework外参照プラグイン登録処理
     * @param plg プラグイン名
     */
    public void addRefPlugin(JavaPlugin plg) {
        otherMap.put(plg.getName(), plg);
        log.info("Add reference [" + plg.getName() + "] -> [" + this.getName() + "]");
    }

    /**
     * 参照プラグイン登録処理
     * @param plg プラグイン名
     */
    public void addRefPluginFrame(PluginFrame plg) {
        refMap.put(plg.getName(), plg);
        log.info("Add reference [" + plg.getName() + "] -> [" + this.getName() + "]");
    }

    /**
     * 依存プラグイン解除処理
     * @param name プラグイン名
     */
    public void unregisterPluginFrame(String name) {
        // 自分からの参照が切れたことを伝える
        plgMap.get(name).delRefPluginFrame(this);
        plgMap.remove(name);
        log.info("Unregister plugin [" + name + "]");
    }

    /**
     * 参照プラグイン削除処理
     * @param plg プラグイン名
     */
    public void delRefPluginFrame(PluginFrame plg) {
        refMap.remove(plg.getName());
        log.info("Del reference plugin[" + plg.getName() + "] -> [" + this.getName() + "]");
    }

    /**
     * Framework外参照プラグイン削除処理
     * @param plg プラグイン名
     */
    public void delRefPlugin(JavaPlugin plg) {
        otherMap.remove(plg.getName());
        log.info("Del reference plugin[" + plg.getName() + "] -> [" + this.getName() + "]");
        log.log(Level.INFO, "Del reference plugin[{0}] -> [{1}]", new Object[]{plg.getName(), this.getName()});
    }

    /**
     * 被参照プラグインリスト取得処理
     * @return プラグインリスト
     */
    public ArrayList<PluginFrame> getRefPluginList() {
        return new ArrayList<>(refMap.values());
    }

    /**
     * 被依存プラグインの連携再起動のためのサスペンド処理
     * @param plg Bukkit/Spigot プラグインインスタンス
     */
    private void suspendPlugin(JavaPlugin plg) {
        // コンフィグファイルに一時的に被依存プラグインのリストを書き出してunloadする
        ConfigFrame f = getDefaultConfig();
        if (f != null) {
            List<String> str = f.getArrayList("suspend");
            if (str == null) {
                str = new ArrayList<>();
            }
            str.add(plg.getName());
            f.conf.set("suspend", str);
            f.saveConfig();
            getServer().dispatchCommand(getServer().getConsoleSender(), "plugman unload " + plg.getName());
        } else {
            log.warning("他プラグインから参照されていて、かつPlugManによる単体再起動を要する場合にはconfig.ymlファイルが必要です。");
        }
        
    }

    /**
     * コマンド実行処理
     * @param sender コマンド送信者
     * @param cmd コマンドインスタンス
     * @param commandLabel コマンドラベル
     * @param args パラメタ配列
     * @return コマンド成否
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String args[]){
        boolean result = false;
        String name = cmd.getName();

        if (cmdMap.containsKey(cmd.getName().toLowerCase())) {
            result = cmdMap.get(cmd.getName()).execute(sender, args);
        }
        
        return result;
    }

    /**
     * タブコンプリート処理
     * @param sender コマンド送信者インスタンス
     * @param cmd コマンドインスタンス
     * @param string コマンド文字列
     * @param strings パラメタ文字列配列
     * @return 保管文字列配列
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String string, String[] strings) {
        // List<String>を返却するとその順次表示、nullを返すとPlayerの順次表示が行われる
        // なおListに「パラメタ候補+ブランク+次パラメタ」で文字列を設定すると「パラメタ候補」は決定パラメタとして認識される
        // newだけした空のListを返すと候補なしとなりTABを続けて押しても何も起きなくなる

        // 自分の管理するコマンドでない場合には無視する(多分plugin.ymlに書いたコマンドの候補しか来ないが)
        if (!cmdMap.containsKey(string.toLowerCase())) {
            return new ArrayList<>();
        }
        // 管理するコマンドの場合、該当コマンドのCommandFrameに処理を移譲する
        return cmdMap.get(string.toLowerCase()).onTabComplete(sender, cmd, string, strings);
    }

    
//  @Override
//  public void onPluginMessageReceived(String channel, Player player, byte[] message) {
//    if (!channel.equals("BungeeCord")) {
//      return;
//    }
//    ByteArrayDataInput in = ByteStreams.newDataInput(message);
//    String subchannel = in.readUTF();
//    if (subchannel.equals("SomeSubChannel")) {
//      // Use the code sample in the 'Response' sections below to read
//      // the data.
//    }
//  }

    /**
     * リロード通知フレーム登録処理
     * @param frame_ リロード通知可能フレームワーククラスインスタンス
     */
    public void registerNotifiable(ReloadNotifiable frame_) {
        if (!reloadList.contains(frame_)) {
            reloadList.add(frame_);
        }
    }

    /**
     * リロード処理
     * リロード対象フレームをリロードする
     * Configの個別リロードとプラグイン全体のリロードに対応できるよう
     * プラグイン本体と各コンフィグに登録するReloadNotifiableクラスは整理して使用すること
     * 設定にPluginLoggerの有効無効の設定を含めるケースを考慮して、Loggerの停止と破棄、再初期化、起動を行う
     * リロード中はPluginLoggerは使用不可。initializeLogger内でconf参照して起動の有無を決定してregisterすること。
     */
    public void reload() {
        /// Loggerの停止
        stopAllPluginLogger();
        // Loggerの破棄
        dropAllPluginLogger();
        // 通知
        for (ReloadNotifiable frame : reloadList) {
            // ConfigFrameだけbase指定をしてコールする
            if (frame instanceof ConfigFrame) {
                frame.reloadNotify(true);
            } else {
                frame.reloadNotify();
            }
        }
        // Loggerの再登録
        initializeLogger();
        // Loggerの再起動
        for (LoggerFrame l : loggerMap.values()) {
            l.start();
        }
    }

    /**
     * マネージャー設定処理
     * initialize直後のプラグインメインクラスのコンストラクタ内から登録すること。
     * 以降の新規生成した各フレームは各フレームのコンストラクタでメインクラスから取得する。
     * プラグイン停止時に終息処理が必要な処理はManagerFrameがリソースの保持を含め担当する。
     * @param manager_ マネージャーインスタンス
     */
    public void registerManager(ManagerFrame manager_) {
        manager = manager_;
        // 各フレームに登録する
        for (Manageable f : cmdMap.values()) { f.registerManager(manager_); }
        for (Manageable f : confMap.values()) { f.registerManager(manager_); }
        for (Manageable f : dbMap.values()) { f.registerManager(manager_); }
        for (Manageable f : listenerMap.values()) { f.registerManager(manager_); }
        for (Manageable f : timerMap.values()) { f.registerManager(manager_); }
        for (Manageable f : loggerMap.values()) { f.registerManager(manager_); }
    }

    /**
     * 依存プラグイン取得処理
     * @param name プラグイン名
     * @return 
     */
    public PluginFrame getPluginFrame(String name) {
        if (plgMap.containsKey(name)) {
            return plgMap.get(name);
        } else {
            registerPluginFrame(name);
            return getPluginFrame(name);
        }
    }

    /**
     * 設定取得処理
     * @param name 設定名
     * @return コンフィグフレームインスタンス返却
     */
    public ConfigFrame getPluginConfig(String name) {
        return confMap.get(name);
    }

    /**
     * プラグイン設定取得処理
     * @return コンフィグフレームインスタンス返却
     */
    public ConfigFrame getDefaultConfig() {
        return confMap.get("config");
    }
    
    /**
     * 設定登録処理
     * @param frame_ コンフィグフレームインスタンス
     */
    protected void registerPluginConfig(ConfigFrame frame_) {
        confMap.put(frame_.getName(), frame_);
    }

    /**
     * コマンド取得処理
     * @param name コマンド名
     * @return コマンドフレームインスタンス
     */
    public CommandFrame getPluginCommand(String name) {
        return cmdMap.get(name);
    }
    
    /**
     * コマンド登録処理
     * @param frame_ コマンドフレームインスタンス
     */
    protected void registerPluginCommand(CommandFrame frame_) {
        cmdMap.put(frame_.getName(), frame_);
    }

    /**
     * 権限リスト表示
     * @return 権限文字列リスト
     */
    public ArrayList<String> getPermissionList() {
        // コマンド登録がある場合、該当コマンドへ実行を引き継ぐ
        ArrayList<String> list = new ArrayList<>();
        for (CommandFrame c : cmdMap.values()) {
            c.getPermissionList(list);
        }
        return list;
    }

    /**
     * リスナー取得処理
     * @param name リスナー名
     * @return リスナーフレームインスタンス
     */
    public ListenerFrame getPluginListener(String name) {
        return listenerMap.get(name);
    }
    @Deprecated
    public ListenerFrame getPluginListerner(String name) {
        return listenerMap.get(name);
    }
    /**
     * リスナー登録処理
     * @param frame_ リスナーフレームインスタンス
     */
    protected void registerPluginListener(ListenerFrame frame_) {
        listenerMap.put(frame_.getName(), frame_);
    }

    /**
     * タイマー取得処理
     * @param name タイマー名
     * @return タイマーフレームインスタンス
     */
    public TimerFrame getPluginTimer(String name) {
        return timerMap.get(name);
    }
    
    /**
     * タイマー登録処理
     * @param frame_ タイマーフレームインスタンス
     */
    protected void registerPluginTimer(TimerFrame frame_) {
        timerMap.put(frame_.getName(), frame_);
    }

    /**
     * 定期タイマー処理
     * @param name_ 名前
     * @param delay_ 初回実行までのディレイ
     * @param period_ 2回目以降の実行インターバル
     * @return 実行したタスク
     */
    public BukkitTask runTaskTimer(String name_, long delay_, long period_) {
        return timerMap.get(name_).runTaskTimer(this, delay_, period_);
    }

    /**
     * マネージャー取得処理
     * @return マネージャーフレームインスタンス
     */
    public ManagerFrame getManager() {
        return manager;
    }

    /**
     * DB取得処理
     * @param name DB名
     * @return データベースフレームインスタンス
     */
    public DatabaseFrame getDB(String name) {
        return dbMap.get(name);
    }

    /**
     * DB登録処理
     * @param frame_ タイマーフレームインスタンス
     */
    protected void registerPluginDB(DatabaseFrame frame_) {
        dbMap.put(frame_.getName(), frame_);
    }

    /**
     * Logger取得処理
     * @param name Logger名
     * @return Loggerフレームインスタンス
     */
    public LoggerFrame getPluginLogger(String name) {
        return loggerMap.get(name);
    }

    /**
     * Logger登録処理
     * @param frame_ Loggerフレームインスタンス
     */
    protected void registerPluginLogger(LoggerFrame frame_) {
        loggerMap.put(frame_.getName(), frame_);
    }

    /**
     * Logger全停止
     */
    protected void stopAllPluginLogger() {
        for (LoggerFrame f : loggerMap.values()) {
            f.stop();
        }
    }

    /**
     * Logger登録解除
     */
    protected void dropAllPluginLogger() {
        loggerMap.clear();
    }

    /**
     * 設定初期化
     * 使用するyamlファイルを全て登録する
     */
    protected void initializeConfig() {
        // OptionalMethod
    };
    
    /**
     * コマンド初期化
     * 使用するコマンドを全て登録する
     * サブコマンドの階層も本メソッドの登録で決める
     */
    protected void initializeCommand() {
        // OptionalMethod
    }
    
    /**
     * イベントリスナー初期化
     * 使用するリスナを全て登録する
     */
    protected void initializeListener() {
        // OptionalMethod
    }

    /**
     * タイマー初期化
     * 常時可動するタイマーを登録する
     * 局所的に使用するタイマーは本メソッドで登録する必要はないが、
     * 常設タイマは登録しておくとプラグイン終了時にキャンセルされる（多分不要な処理）
     */
    protected void initializeTimer() {
        // OptionalMethod
    }

    /**
     * データベース初期化
     * プラグイン終了時にcloseする
     */
    protected void initializeDB() {
        // OptionalMethod
    }

    /**
     * logger初期化
     * 使用するLoggerを全て登録する
     */
    protected void initializeLogger() {
        // OptionalMethod
    }

}

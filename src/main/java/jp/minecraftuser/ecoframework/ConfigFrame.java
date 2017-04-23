
package jp.minecraftuser.ecoframework;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import jp.minecraftuser.ecoframework.iface.Manageable;
import jp.minecraftuser.ecoframework.iface.ReloadNotifiable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * コンフィグフレームワーククラス
 * @author ecolight
 */
public abstract class ConfigFrame implements Manageable, ReloadNotifiable {
    private final ArrayList<ReloadNotifiable> reloadList = new ArrayList<>();
    protected final Logger log;
    protected final PluginFrame plg;
    protected final String name;
    protected final String filename;
    protected final HashMap<String, Set> listSectionString = new HashMap<>();;
    protected final HashMap<String, List> listArrayString = new HashMap<>();
    protected final HashMap<String, String> listString = new HashMap<>();
    protected final HashMap<String, Integer> listInt = new HashMap<>();
    protected final HashMap<String, Long> listLong = new HashMap<>();
    protected final HashMap<String, Boolean> listBoolean = new HashMap<>();
    protected File file;
    protected FileConfiguration conf;
    protected ManagerFrame manager = null;

    /**
     * コンストラクタ
     * @param plg_ プラグインインスタンス
     */
    public ConfigFrame(PluginFrame plg_) {
        plg = plg_;
        log = plg.getLogger();
        manager = plg.getManager();
        name = "config";
        filename = "config.yml";
        file = new File(plg.getDataFolder(), filename);
        conf = plg.getConfig();
        
        reloadNotify(true);
    }
    
    /**
     * コンストラクタ
     * @param plg_ プラグインインスタンス
     * @param filename_ ファイル名
     * @param name_ 名前
     */
    public ConfigFrame(PluginFrame plg_, String filename_, String name_) {
        plg = plg_;
        log = plg.getLogger();
        manager = plg.getManager();
        filename = filename_;
        name = name_;
        if (!plg.getDataFolder().exists()) plg.getDataFolder().mkdir();
        file = new File(plg.getDataFolder(), filename);
        conf = YamlConfiguration.loadConfiguration(file);
        
        reloadNotify(true);
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
     * リロード通知フレーム登録処理
     * @param frame_ リロード通知可能フレームワーククラスインスタンス
     */
    public void registerNotifiable(ReloadNotifiable frame_) {
        if (!reloadList.contains(frame_)) {
            reloadList.add(frame_);
        }
    }
    
    /**
     * 名前取得
     * @return 名前
     */
    public String getName() {
        return name;
    }

    /**
     * デフォルト値ロード
     */
    private void defLoad() {
        try {
            Reader def = new InputStreamReader(plg.getResource(filename), "UTF8") ;
            YamlConfiguration defCnf = YamlConfiguration.loadConfiguration(def);
            conf.setDefaults(defCnf);
        } catch (UnsupportedEncodingException ex) {
            log.warning(ex.getLocalizedMessage());
        }
    }

    /**
     * リロード処理
     * ネストしたデータの再登録機構はまったく考えていないのであんまり複雑な構成には対応できない。
     * ネストするようなデータはyamlで管理するなと言う戒め
     */    
    public void reload() {
        // 破棄
        file = new File(plg.getDataFolder(), filename);
        conf = YamlConfiguration.loadConfiguration(file);

        // 再登録・取得
        for (String s: listSectionString.keySet().toArray(new String[0])) {
            registerSectionString(s);
        }
        for (String s: listArrayString.keySet().toArray(new String[0])) {
            registerArrayString(s);
        }
        for (String s: listString.keySet().toArray(new String[0])) {
            registerString(s);
        }
        for (String s: listLong.keySet().toArray(new String[0])) {
            registerLong(s);
        }
        for (String s: listInt.keySet().toArray(new String[0])) {
            registerInt(s);
        }
        for (String s: listBoolean.keySet().toArray(new String[0])) {
            registerBoolean(s);
        }
        
        // copy
        conf.options().copyDefaults(true);
        saveConfig();
        
        // 通知
        // 自分を呼んでから他の通知先の基底処理を呼ぶ
        reloadNotify(false);
        for (ReloadNotifiable frame : reloadList) {
            // ConfigFrameだけbase指定をしてコールする
            if (frame instanceof ConfigFrame) {
                frame.reloadNotify(true);
            } else {
                frame.reloadNotify();
            }
        }
    }

    @Override
    public void reloadNotify(boolean base) {
        if (base) {
            reload();
        }
    }
        
    /**
     * セーブ処理
     */
    public final void saveConfig() {
        defLoad();
        try {
            conf.save(file);
        } catch (IOException ex) {
            log.warning(ex.getLocalizedMessage());
        }
    }
    
    /**
     * セクション文字列配列設定名登録
     * 指定したkey配下の要素の一覧を取得する
     * @param key キー値
     * @param param [0]:quietフラグ
     * @return コンフィグ値取得成否
     */
    public boolean registerSectionString(String key, boolean... param) {
        Set<String> set;
        // 指定がない場合はルートセクションの要素を一覧にする
        if ((key == null) || (key.length() == 0)) {
            set = conf.getKeys(false);
        }
        // 指定がある場合は指定セクション配下の要素を一覧にする
        else {
            set = conf.getConfigurationSection(key).getKeys(false);
        }
        if (key == null) {
            if ((param.length == 0) || (!param[0])) {
                log.warning("指定された設定が取得できませんでした["+key+"]");
            }
            return false;
        }
        if (listSectionString.containsKey(key)) {
            listSectionString.remove(key);
        }
        listSectionString.put(key, set);
        return true;
    }
    /**
     * セクション文字列配列取得
     * @param key キー値
     * @return 文字列配列
     */
    public Set<String> getSectionList(String key) {
        return listSectionString.get(key);
    }

    /**
     * 文字列配列設定名登録
     * @param key キー値
     * @param param [0]:quietフラグ
     * @return コンフィグ値取得成否
     */
    public boolean registerArrayString(String key, boolean... param) {
        List list = conf.getStringList(key);
        if (list == null) {
            if ((param.length == 0) || (!param[0])) {
                log.warning("指定された設定が取得できませんでした["+key+"]");
            }
            return false;
        }
        if (listArrayString.containsKey(key)) {
            listArrayString.remove(key);
        }
        listArrayString.put(key, list);
        return true;
    }
    /**
     * 文字列配列取得
     * @param key キー値
     * @return 文字列配列
     */
    public List<String> getArrayList(String key) {
        return listArrayString.get(key);
    }

    /**
     * 文字列設定名登録
     * @param key キー値
     * @param param [0]:quietフラグ
     * @return コンフィグ値取得成否
     */
    public boolean registerString(String key, boolean... param) {
        String str = conf.getString(key);
        if (str == null) {
            if ((param.length == 0) || (!param[0])) {
                log.warning("指定された設定が取得できませんでした["+key+"]");
            }
            return false;
        }
        if (listString.containsKey(key)) {
            listString.remove(key);
        }
        listString.put(key, str);
        return true;
    }
    /**
     * 文字列取得
     * @param key キー値
     * @return 文字列
     */
    public String getString(String key) {
        return listString.get(key);
    }

    /**
     * Long値設定名登録
     * @param key キー値
     * @param param [0]:quietフラグ
     * @return コンフィグ値取得成否
     */
    public boolean registerLong(String key, boolean... param) {
        if (!conf.contains(key)) {
            if ((param.length == 0) || (!param[0])) {
                log.warning("指定された設定が取得できませんでした["+key+"]");
            }
            return false;
        }
        long num = conf.getLong(key);
        if (listLong.containsKey(key)) {
            listLong.remove(key);
        }
        listLong.put(key, num);
        return true;
    }
    /**
     * Long値取得
     * @param key キー値
     * @return Long値
     */
    public Long getLong(String key) {
        return listLong.get(key);
    }

    /**
     * Int値設定名登録
     * @param key キー値
     * @param param [0]:quietフラグ
     * @return コンフィグ値取得成否
     */
    public boolean registerInt(String key, boolean... param) {
        if (!conf.contains(key)) {
            if ((param.length == 0) || (!param[0])) {
                log.warning("指定された設定が取得できませんでした["+key+"]");
            }
            return false;
        }
        int num = conf.getInt(key);
        if (listInt.containsKey(key)) {
            listInt.remove(key);
        }
        listInt.put(key, num);
        return true;
    }
    /**
     * Int値取得
     * @param key キー値
     * @return Int値
     */
    public Integer getInt(String key) {
        return listInt.get(key);
    }

    /**
     * Boolean値設定名登録
     * @param key キー値
     * @param param [0]:quietフラグ
     * @return コンフィグ値取得成否
     */
    public boolean registerBoolean(String key, boolean... param) {
        if (!conf.contains(key)) {
            if ((param.length == 0) || (!param[0])) {
                log.warning("指定された設定が取得できませんでした["+key+"]");
            }
            return false;
        }
        boolean b = conf.getBoolean(key);
        if (listBoolean.containsKey(key)) {
            listBoolean.remove(key);
        }
        listBoolean.put(key, b);
        return true;
    }
    /**
     * Boolean値取得
     * @param key キー値
     * @return Boolean値
     */
    public Boolean getBoolean(String key) {
        return listBoolean.get(key);
    }

    /**
     * コンフィグ取得
     * @return コンフィグ
     */
    public FileConfiguration getConf() {
        return conf;
    }

}

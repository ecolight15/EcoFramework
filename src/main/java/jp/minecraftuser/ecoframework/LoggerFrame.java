
package jp.minecraftuser.ecoframework;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import jp.minecraftuser.ecoframework.iface.Manageable;
import org.apache.commons.lang.RandomStringUtils;

/**
 * ログインスレッドフレームワーククラス
 * @author ecolight
 */
public class LoggerFrame implements Manageable{
    protected SimpleDateFormat sd = new SimpleDateFormat("[yyyy/MM/dd HH:mm:ss.SSS]");
    protected String name;
    protected File file;
    protected ManagerFrame manager = null;
    protected final PluginFrame plg;
    protected final ConfigFrame conf;
    protected Logger log;
    private LinkedBlockingQueue<String> logque;
    private Thread thread;
    private boolean isActive = true;
    
    /**
     * コンストラクタ
     * @param plg_ プラグインインスタンス
     * @param name_ Logger名
     */
    public LoggerFrame(PluginFrame plg_, String name_) {
        name = name_;
        plg = plg_;
        log = plg.getLogger();
        conf = plg.getDefaultConfig();
        setFileName(null);
        logque = new LinkedBlockingQueue<>();
    }

    /**
     * ファイル名指定つきコンストラクタ
     * @param plg_ プラグインインスタンス
     * @param name_ Logger名
     */
    public LoggerFrame(PluginFrame plg_, String filename, String name_) {
        name = name_;
        plg = plg_;
        log = plg.getLogger();
        conf = plg.getDefaultConfig();
        setFileName(filename);
        logque = new LinkedBlockingQueue<>();
        
    }

    /**
     * ファイル名設定メソッド
     * 名前指定あり : plugin/(plugin_name)/name_
     * 名前指定なし : plugin/(plugin_name)/(random_str).txt
     * @param name_ 名前指定
     */
    protected final void setFileName(String name_) {
        String base = plg.getDataFolder().getAbsolutePath();
        if (name_ != null) {
            file = new File(base + "/" + name_);
        } else {
            String path;
            while (true) {
                path = base + "/" + RandomStringUtils.randomAlphanumeric(16) + ".txt";
                if (!new File(path).exists()) break;
            }
            file = new File(path);
        }
    }

    /**
     * DateFormat設定
     * @param format フォーマット文字列
     */
    protected void setDateFormat(String format) {
        sd = new SimpleDateFormat(format);
    }

    /**
     * Logger処理スレッド起動
     */
    public void start() {
        thread = new Thread(() -> {
            Logger.getLogger(LoggerFrame.class.getName()).log(Level.INFO, String.format("Start %s logging thread", name));
            while (isActive) {
                try {
                    String work = logque.take();
                    // try-with-resources による自動flush&close
                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
                        String token_message = sd.format(new Date()) + work;
                        bw.write(token_message);
                        bw.newLine();
                    }
                } catch (InterruptedException ex) {
                    // 外からのinterruptによるtakeの終了
                    break;
                } catch (IOException ex) {
                    Logger.getLogger(LoggerFrame.class.getName()).log(Level.SEVERE, null, ex);
                    break;
                }
            }
            Logger.getLogger(LoggerFrame.class.getName()).log(Level.INFO, String.format("Stop %s logging thread", name));
        });
    }

    /**
     * Logger処理スレッド停止
     */
    public void stop() {
        isActive = false;
        thread.interrupt();
    }    

    /**
     * ログ文字列追加
     * @param str ログ文字列
     */
    public void log(String str) {
        logque.add(str);
    }
    
    /**
     * ログ文字列追加
     * @param str ログ文字列のビルダー
     */
    public void log(StringBuilder str) {
        logque.add(str.toString());
    }
    
    /**
     * 名前取得
     * @return 名前
     */
    public String getName() {
        return name;
    }

    /**
     * マネージャー登録処理
     * @param manager_ マネージャーフレームインスタンス
     */
    @Override
    public void registerManager(ManagerFrame manager_) {
        manager = manager_;
    }
    
    
}

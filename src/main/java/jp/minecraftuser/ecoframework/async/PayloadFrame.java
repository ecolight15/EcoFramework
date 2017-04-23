
package jp.minecraftuser.ecoframework.async;

import java.io.Serializable;
import jp.minecraftuser.ecoframework.PluginFrame;
import jp.minecraftuser.ecoframework.iface.ReloadNotifiable;

/**
 * メインスレッドと非同期スレッド間のデータ送受用クラス
 * @author ecolight
 */
public abstract class PayloadFrame implements Serializable, ReloadNotifiable {
    private String name = "";
    private long major = -1;
    private long minor = -1;
    private long revision = -1;
    private boolean snapshot = false;

    /**
     * コンストラクタ
     * @param plg_ プラグインインスタンス(ただし通信に用いられる可能性を念頭に一定以上の情報は保持しない)
     */
    public PayloadFrame(PluginFrame plg_) {
        name = plg_.getDescription().getName();
        String[] versions = plg_.getDescription().getVersion().split("[. -]");
        for (String s : versions) {
            try {
                if (s.equalsIgnoreCase("snapshot")) {
                    snapshot = true;
                    continue;
                }
                long num = Long.parseLong(s);
                if (major == -1) {
                    major = num;
                } else if (minor == -1) {
                    minor = num;
                } else if (revision == -1) {
                    revision = num;
                }
            } catch (NumberFormatException e) {
                plg_.getLogger().info(e.getLocalizedMessage());
                plg_.getLogger().info("Payload:version解析不能文字検出[" + s);
            }
        }
    }

    /**
     * フレーム送出プラグイン名
     * @return プラグイン名
     */
    public String getName() {
        return name;
    }
    /**
     * フレーム送出プラグインバージョン(Major)
     * @return バージョン(Major)
     */
    public long getMajorVersion() {
        return major;
    }
    /**
     * フレーム送出プラグインバージョン(Minor)
     * @return バージョン(Minor)
     */
    public long getMinorVersion() {
        return minor;
    }
    /**
     * フレーム送出プラグインバージョン(Revision)
     * @return バージョン(Reivision)
     */
    public long getRevision() {
        return revision;
    }
    /**
     * フレーム送出プラグインSnapshot判定
     * @return Snapshot判定
     */
    public boolean isSnapshot() {
        return snapshot;
    }
    /**
     * フレーム送出プラグインバージョン取得処理
     * @return バージョン
     */
    public String getVersion() {
        StringBuilder sb = new StringBuilder();
        if (major != -1) {
            sb.append(major);
        }
        if (minor != -1) {
            sb.append(".");
            sb.append(minor);
        }
        if (revision != -1) {
            sb.append(".");
            sb.append(revision);
        }
        if (snapshot) {
            sb.append("-SNAPSHOT");
        }
        return sb.toString();
    }
}

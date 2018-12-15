
package jp.minecraftuser.ecoframework.store;

import java.io.File;

/**
 *
 * @author ecolight
 */
public class PlayerFileSet {
    public File profile;
    public File stats;
    public File adv;
    public PlayerFileSet(File p, File s, File a) {
        profile = p;
        stats = s;
        adv = a;
    }
}

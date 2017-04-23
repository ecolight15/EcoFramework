
package jp.minecraftuser.ecoframework.exception;

/**
 * データベース操作失敗 プラグイン側で使うよう
 * @author ecolight
 */
public class DatabaseControlException extends Exception{

    public DatabaseControlException(String a) {
        super(a);
    }
    
}

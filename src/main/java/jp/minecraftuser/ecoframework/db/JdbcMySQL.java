
package jp.minecraftuser.ecoframework.db;

import com.zaxxer.hikari.HikariConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import jp.minecraftuser.ecoframework.PluginFrame;

/**
 *
 * @author ecolight
 */
public class JdbcMySQL extends JdbcBase {
    public JdbcMySQL(PluginFrame plg_, String dbname_, String addr_, String user_, String pass_) throws ClassNotFoundException, SQLException {
        super(plg_, addr_, user_, pass_, dbname_);
    }

    @Override
    protected String setDriver() {
        return "com.mysql.jdbc.Driver";
    }

    @Override
    protected String connectDB() {
        // 指定された名前のdbを接続する。
        return "jdbc:mysql://"+addr+"/"+dbname+"?enabledTLSProtocols=TLSv1.2";
    }

    @Override
    protected void registerConfig(HikariConfig config) {
        // エンコーディング設定
        config.addDataSourceProperty("characterEncoding", "utf8");
        config.addDataSourceProperty("useUnicode", "true");
        // ユーザーパス設定
        config.addDataSourceProperty("user", user);
        config.addDataSourceProperty("password", pass);
        // とりあえずキャッシュ系設定やサーバーサイドプリペアドステートメント設定は省略
        // 必要あれば継承してオーバーライドすること
        // config.addDataSourceProperty("cachePrepStmts", "true");
        // config.addDataSourceProperty("prepStmtCacheSize", "250");
        // config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        // config.addDataSourceProperty("useServerPrepStmts", "true");
    }

    @Override
    public boolean constans(String table_) throws SQLException {
        PreparedStatement prep = null;
        ResultSet rs = null;
        Connection con = connect();
        boolean result = false;
        try {
            prep = con.prepareStatement("SELECT * FROM information_schema.columns WHERE table_name = ?");
            prep.setString(1, table_);
            rs = prep.executeQuery();
            result = rs.next();
            rs.close();
            prep.close();
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
        return result;
    }
    
    @Override
    public boolean constans(String table_, String column_) throws SQLException {
        PreparedStatement prep;
        ResultSet rs;
        Connection con = connect();
        boolean result;
        try {
            prep = con.prepareStatement("SELECT * FROM information_schema.columns WHERE table_name = ? AND column_name = ?");
            prep.setString(1, table_);
            prep.setString(2, column_);
            rs = prep.executeQuery();
            result = rs.next();
            rs.close();
            prep.close();
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
        return result;
    }
}

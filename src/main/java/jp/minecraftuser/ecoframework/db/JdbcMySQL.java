
package jp.minecraftuser.ecoframework.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import jp.minecraftuser.ecoframework.PluginFrame;

/**
 *
 * @author ecolight
 */
public class JdbcMySQL extends JdbcBase {
    private final String addr;
    private final String user;
    private final String pass;
    public JdbcMySQL(PluginFrame plg_, String dbname_, String addr_, String user_, String pass_) throws ClassNotFoundException, SQLException {
        super(plg_, dbname_);
        addr = addr_;
        user = user_;
        pass = pass_;
    }

    @Override
    protected String setDriver() {
        return "com.mysql.jdbc.Driver";
    }

    @Override
    protected String connectDB() {
        // conを受け取らない場合、指定された名前のdbを接続する。
        return "jdbc:mysql://"+addr+"/?useUnicode=true&characterEncoding=utf8&user="+user+"&password="+pass;
    }

    @Override
    protected void afterConnection() throws SQLException {
        try {
            // まずDB接続を試みる
            executeStatement("use " + dbname);
        } catch (SQLException ex) {
            // 失敗したらDBが無いものとして新規作成する
            executeStatement("create database " + dbname + " character set utf8");
            con.commit();
            executeStatement("use " + dbname);
            // ここでこけたらDB無いし何も権限無い可能性がある。
            throw ex;
        }
        
    }

    @Override
    public boolean constans(String table_) throws SQLException {
        PreparedStatement prep = null;
        ResultSet rs = null;
        boolean result = false;
        prep = con.prepareStatement("SELECT * FROM information_schema.columns WHERE table_name = ?");
        prep.setString(1, table_);
        rs = prep.executeQuery();
        result = rs.next();
        rs.close();
        prep.close();
        return result;
    }
    
    @Override
    public boolean constans(String table_, String column_) throws SQLException {
        PreparedStatement prep;
        ResultSet rs;
        boolean result;
        prep = con.prepareStatement("SELECT * FROM information_schema.columns WHERE table_name = ? AND column_name = ?");
        prep.setString(1, table_);
        prep.setString(2, column_);
        rs = prep.executeQuery();
        result = rs.next();
        rs.close();
        prep.close();
        return result;
    }
}

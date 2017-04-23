
package jp.minecraftuser.ecoframework.db;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import jp.minecraftuser.ecoframework.PluginFrame;

/**
 *
 * @author ecolight
 */
public class JdbcSqlite extends JdbcBase {
    private String dbpath;
    public JdbcSqlite(PluginFrame plg_, String dbname_) throws ClassNotFoundException, SQLException {
        super(plg_, dbname_);
    }

    @Override
    protected void preConnection() {
        if (!plg.getDataFolder().exists()) plg.getDataFolder().mkdir();
        // DBのopenかcreateか(既にファイルが存在するかどうか)を保持しておく
        dbpath = plg.getDataFolder().getPath() + "/" + dbname;
        if (!new File(dbpath).exists()) {
            justCreated = true;
        }
    }

    @Override
    protected String setDriver() {
        return "org.sqlite.JDBC";
    }

    @Override
    protected void registerProperty() {
        setProperty("foreign_keys", "true"); // 外部制約キーの有効化
    }

    @Override
    protected String connectDB() {
        // conを受け取らない場合、指定された名前のdbを接続する。
        return "jdbc:sqlite:" + dbpath;
    }
    
    @Override
    public boolean constans(String table_) throws SQLException {
        PreparedStatement prep = null;
        ResultSet rs = null;
        boolean result = false;
        prep = con.prepareStatement("SELECT name FROM sqlite_master WHERE type = 'table' AND name = ? ;");
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
        prep = con.prepareStatement("SELECT sql FROM sqlite_master WHERE type = 'table' AND name = ? ;");
        prep.setString(1, table_);
        rs = prep.executeQuery();
        result = rs.next();
        // 結果無しならfalse復帰
        if (result) {
            result = false;
            String sql = rs.getString("sql");
            String work = sql.substring(sql.indexOf("(")+1,sql.length());
            // ()内のcolumnの一覧から
            for (String s : work.replaceAll(")", "").split(",")) {
                // 先頭に空白が入る場合があるので、取りつつ一つ目の" "より前の文字列を検査する
                if (s.trim().substring(0, s.indexOf(" ")).equalsIgnoreCase(column_)) {
                    result = true;
                    break;
                }
            }
        }
        rs.close();
        prep.close();
        return result;
    }
}

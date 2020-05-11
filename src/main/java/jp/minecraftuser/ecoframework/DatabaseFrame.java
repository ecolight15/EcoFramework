package jp.minecraftuser.ecoframework;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.logging.Logger;
import jp.minecraftuser.ecoframework.db.CTYPE;
import jp.minecraftuser.ecoframework.db.JdbcBase;
import jp.minecraftuser.ecoframework.db.JdbcMySQL;
import jp.minecraftuser.ecoframework.db.JdbcSqlite;
import jp.minecraftuser.ecoframework.iface.Manageable;
import jp.minecraftuser.ecoframework.iface.ReloadNotifiable;

/**
 * データベースフレームクラス
 * @author ecolight
 */
abstract public class DatabaseFrame implements ReloadNotifiable, Manageable {
    protected final PluginFrame plg;
    protected final Logger log;
    protected final ConfigFrame conf;
    protected final String dbname;
    protected final String name;
    protected final String addr;
    protected final String user;
    protected final String pass;
    protected ManagerFrame manager = null;
    // 以下は最初にopenした親インスタンスのみ保持する情報
    protected JdbcBase jdbc = null;
    protected long dbversion = 0;
    protected boolean justCreated = false;
    
    /**
     * コンストラクタ兼Sqliteデータベース接続生成処理
     * @param plg_ プラグインインスタンス
     * @param dbname_ データベース名
     * @param name_ データベース名
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public DatabaseFrame(PluginFrame plg_, String dbname_, String name_) throws ClassNotFoundException, SQLException {
        plg = plg_;
        log = plg.getLogger();
        conf = plg.getDefaultConfig();
        manager = plg.getManager();
        dbname = dbname_;
        name = name_;
        addr = "";
        user = "";
        pass = "";

        // DB接続処理
        jdbc = new JdbcSqlite(plg_, dbname_);
        justCreated = jdbc.isJustCreated();
        migration();
    }

    /**
     * コンストラクタ兼MySQLデータベース接続生成処理
     * @param plg_ プラグインインスタンス
     * @param dbname_ データベース名
     * @param name_ データベース名
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public DatabaseFrame(PluginFrame plg_, String addr_, String user_, String pass_, String dbname_, String name_) throws ClassNotFoundException, SQLException {
        plg = plg_;
        addr = addr_;
        user = user_;
        pass = pass_;
        log = plg.getLogger();
        conf = plg.getDefaultConfig();
        manager = plg.getManager();
        dbname = dbname_;
        name = name_;

        // DB接続処理
        jdbc = new JdbcMySQL(plg, dbname, addr, user, pass);
        justCreated = jdbc.isJustCreated();
        migration();
    }

    /**
     * コネクションプールからのコネクション取得処理
     * @return コネクションを返却する
     * @throws SQLException 
     */
    public Connection connect() throws SQLException {
        return jdbc.connect();
    }

    /**
     * マイグレーション呼び出し処理
     * @throws SQLException 
     */
    private void migration() throws SQLException {
        // フレームワーク固有マイグレーション処理
        log.info("Start migration database.[" + dbname + "]");
        Connection con = connect();
        try {
            // DB設定テーブルのチェック
            if (!isExistSettings()) {
                // 無い場合は作成する
                log.info("Start create framework settings.[" + dbname + "]");
                createSettings(con);
                log.info("Created framework settings.[" + dbname + "]");
            } else {
                log.info("Exist framework settings.[" + dbname + "]");
            }
            // DBバージョンを保持しておく
            dbversion = getSettingsVersion(con);
            log.info("current " + dbname + " version is "+dbversion);
            // migration処理を呼ぶ
            migrationData(con);
            con.commit();
            con.close();
        } catch (SQLException ex) {
            // 失敗したら
            con.rollback();
            con.close();
            log.warning(ex.getLocalizedMessage());
            log.warning(ex.getSQLState());
            throw new SQLException("Database migration failed. ");
        }
        log.info("after migration " + dbname + " version is "+dbversion);
        log.info("database migration complete.[" + dbname + "]");
    }
    
    /**
     * コンストラクタ兼データベース登録処理
     * @param frame_ データベースフレーム
     */
    public DatabaseFrame(DatabaseFrame frame_) {
        plg = frame_.getPluginFrame();
        log = plg.getLogger();
        conf = plg.getDefaultConfig();
        manager = plg.getManager();
        name = frame_.getName();
        dbname = frame_.getDBName();
        addr = "";
        user = "";
        pass = "";
        // DBフレームインスタンスを受領した場合には対象コネクションに対する操作を実施する
        jdbc = frame_.getJdbc();
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
     * プラグインの取得
     * @return プラグインインスタンス
     */
    private PluginFrame getPluginFrame() {
        return plg;
    }
    
    /**
     * Jdbcラッパーの取得
     * @return Jdbcラッパーインスタンスの返却
     */
    private JdbcBase getJdbc() {
        return jdbc;
    }
    
    /**
     * データベース名の取得
     * @return データベース名
     */
    protected String getName() {
        return name;
    }

    /**
     * データベースファイル名の取得
     * @return データベースファイル名
     */
    private String getDBName() {
        return dbname;
    }

    /**
     * データベースインスタンス破棄処理
     * 本クラスをインスタンス化した場合破棄するタイミングで必ず呼ぶこと。finalizeには頼らない。
     * 自分が生成したDBのみ処理する
     */
    public void close() {
        if (jdbc != null) {
            jdbc.close();
        }
    }

    /**
     * データベース移行処理
     * 内部処理からトランザクション開始済みの状態で呼ばれる
     * @throws SQLException
     */
    protected void migrationData(Connection con) throws SQLException {
        // optional method
    };


//===== DB共通操作 ==============================================================

    /**
     * ステートメント実行処理
     * SQLインジェクション防止のためpreparedステートメント使用せずにユーザー入力文字列をコミットしないこと
     * @param con_ コネクション
     * @param sql_ 実行SQL文
     * @throws SQLException
     */
    public void executeStatement(Connection con_, String sql_) throws SQLException {
        Statement stmt;
        stmt = con_.createStatement();
        stmt.executeUpdate(sql_);
        stmt.close();
    }

    /**
     * ステートメント実行処理
     * SQLインジェクション防止のためpreparedステートメント使用せずにユーザー入力文字列をコミットしないこと
     * @param sql_ 実行SQL文
     * @throws SQLException
     */
    public void executeStatement(String sql_) throws SQLException {
        Connection con = connect();
        try {
            executeStatement(con, sql_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    }

    /**
     * クエリ実行処理(復帰データ有)
     * @param sql_ 実行SQL文
     * @return 実行結果
     * @throws SQLException
     */
    public ResultSet executeQuery(Connection con_, String sql_) throws SQLException {
        Statement stmt;
        ResultSet rs = null;
        stmt = con_.createStatement();
        rs = stmt.executeQuery(sql_);
        stmt.close();
        return rs;
    }

    /**
     * クエリ実行処理(復帰データ有)
     * @param sql_ 実行SQL文
     * @return 実行結果
     * @throws SQLException
     */
    public ResultSet executeQuery(String sql_) throws SQLException {
        Connection con = connect();
        ResultSet rs = null;
        try {
            rs = executeQuery(con, sql_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
        return rs;
    }

//===== DB共通ユーティリティ ====================================================

    /**
     * テーブルの存在チェック
     * @param table_ 検索テーブル名
     * @return 有無を示すboolean値
     * @throws SQLException 
     */
    public boolean constans(String table_) throws SQLException {
        return jdbc.constans(table_);
    }

    /**
     * カラムの存在チェック
     * @param table_ テーブル名
     * @param column_ カラム名
     * @return 有無を示すboolean値(テーブルもカラムもある場合のみtrue)
     * @throws SQLException 
     */
    public boolean constans(String table_, String column_) throws SQLException {
        return jdbc.constans(table_, column_);
    }

    public long count(Connection con_, String table_, String column_) throws SQLException {
        PreparedStatement prep;
        ResultSet rs;
        boolean result;
        long count = 0;
        prep = con_.prepareStatement("SELECT COUNT(?) FROM " + table_);
        prep.setString(1, column_);
        rs = prep.executeQuery();
        result = rs.next();
        // 結果無しなら0復帰
        if (result) {
            count = rs.getLong(1);
        }
        rs.close();
        prep.close();
        return count;
    }

    public long count(String table_, String column_) throws SQLException {
        long count = 0;
        Connection con = connect();
        try {
            count = count(con, table_, column_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
        return count;
    }

    public long count(Connection con_, String table_) throws SQLException {
        return count(con_, table_, "*");
    }

    public long count(String table_) throws SQLException {
        return count(table_, "*");
    }

    public void renameTable(Connection con_, String table_, String name_) throws SQLException {
        executeStatement(con_, "ALTER TABLE " + table_ + " RENAME TO " + name_);
    }

    public void renameTable(String table_, String name_) throws SQLException {
        executeStatement("ALTER TABLE " + table_ + " RENAME TO " + name_);
    }

    public void dropTable(Connection con_, String name_) throws SQLException {
        executeStatement(con_, "DROP TABLE " + name_);
        // VACUUMはSQLite向けのコマンドなのでSQLiteの場合のみ実行する
        if (jdbc instanceof JdbcSqlite) {
            executeStatement(con_, "VACUUM;");
        }
    }

    public void dropTable(String name_) throws SQLException {
        executeStatement("DROP TABLE " + name_);
        // VACUUMはSQLite向けのコマンドなのでSQLiteの場合のみ実行する
        if (jdbc instanceof JdbcSqlite) {
            executeStatement("VACUUM;");
        }
    }

    public void deleteRecordByString(Connection con_, String table_, String keycolumn_, String key_) throws SQLException {
        PreparedStatement prep = null;
        prep = con_.prepareStatement("DELETE FROM " + table_ + " WHERE " + keycolumn_ + " = ?");
        prep.setString(1, key_);
        prep.executeQuery();
        prep.close();
    }

    public void deleteRecordByString(String table_, String keycolumn_, String key_) throws SQLException {
        Connection con = connect();
        try {
            deleteRecordByString(con, table_, keycolumn_, key_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    }

    public void deleteRecordByLong(Connection con_, String table_, String keycolumn_, long key_) throws SQLException {
        PreparedStatement prep = null;
        prep = con_.prepareStatement("DELETE FROM " + table_ + " WHERE " + keycolumn_ + " = ?");
        prep.setLong(1, key_);
        prep.executeQuery();
        prep.close();
    }

    public void deleteRecordByLong(String table_, String keycolumn_, long key_) throws SQLException {
        Connection con = connect();
        try {
            deleteRecordByLong(con, table_, keycolumn_, key_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    }

    public void addLongColumn(Connection con_, String table_, String column_, Long default_) throws SQLException {
        PreparedStatement prep = null;
        if (default_ == null) {
            prep = con_.prepareStatement("ALTER TABLE " + table_ + " ADD COLUMN " + column_ + " INTEGER");
        } else {
            prep = con_.prepareStatement("ALTER TABLE " + table_ + " ADD COLUMN " + column_ + " INTEGER DEFAULT ? NOT NULL");
        }
        if (default_ != null) {
           prep.setLong(1, default_);
        }
        prep.executeQuery();
        prep.close();
    }

    public void addLongColumn(String table_, String column_, Long default_) throws SQLException {
        Connection con = connect();
        try {
            addLongColumn(con, table_, column_, default_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    }

    public void addFloatColumn(Connection con_, String table_, String column_, Float default_) throws SQLException {
        PreparedStatement prep = null;
        if (default_ == null) {
            prep = con_.prepareStatement("ALTER TABLE " + table_ + " ADD COLUMN " + column_ + " REAL");
        } else {
            prep = con_.prepareStatement("ALTER TABLE " + table_ + " ADD COLUMN " + column_ + " REAL DEFAULT ? NOT NULL");
        }
        if (default_ != null) {
           prep.setFloat(1, default_);
        }
        prep.executeQuery();
        prep.close();
    }

    public void addFloatColumn(String table_, String column_, Float default_) throws SQLException {
        Connection con = connect();
        try {
            addFloatColumn(con, table_, column_, default_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    }

    public void addStringColumn(Connection con_, String table_, String column_, String default_) throws SQLException {
        PreparedStatement prep = null;
        if (default_ == null) {
            prep = con_.prepareStatement("ALTER TABLE " + table_ + " ADD COLUMN " + column_ + " TEXT");
        } else {
            prep = con_.prepareStatement("ALTER TABLE " + table_ + " ADD COLUMN " + column_ + " TEXT DEFAULT ? NOT NULL");
        }
        if (default_ != null) {
           prep.setString(1, default_);
        }
        prep.executeQuery();
        prep.close();
    }

    public void addStringColumn(String table_, String column_, String default_) throws SQLException {
        Connection con = connect();
        try {
            addStringColumn(con, table_, column_, default_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    }

    public void addBlobColumn(Connection con_, String table_, String column_, byte[] default_) throws SQLException {
        PreparedStatement prep = null;
        if (default_ == null) {
            prep = con_.prepareStatement("ALTER TABLE " + table_ + " ADD COLUMN " + column_ + " BLOB");
        } else {
            prep = con_.prepareStatement("ALTER TABLE " + table_ + " ADD COLUMN " + column_ + " BLOB DEFAULT ? NOT NULL");
        }
        if (default_ != null) {
           prep.setBytes(1, default_);
        }
        prep.executeQuery();
        prep.close();
    }

    public void addBlobColumn(String table_, String column_, byte[] default_) throws SQLException {
        Connection con = connect();
        try {
            addBlobColumn(con, table_, column_, default_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    }

    public void createIndex(Connection con_, String name_, String table_, boolean unique_, String... columns_) throws SQLException {
        if (columns_.length == 0) {
            throw new SQLException("インデックス作成に必要なパラメタが不足しています");
        }
        // 指定カラムの数だけ "?," を用意する
        StringBuilder sb = new StringBuilder();
        if (unique_) {
            sb.append("CREATE INDEX " + name_ + " ON " + table_ + " (");
        } else {
            sb.append("CREATE UNIQUE INDEX " + name_ + " ON " + table_ + " (");
        }
        for (int i = 0; i < columns_.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append("?");
        }
        sb.append(");");
        PreparedStatement prep = null;
        prep = con_.prepareStatement(sb.toString());
        int paramIndex = 1;
        for (String column : columns_) {
           prep.setString(paramIndex, column);
           paramIndex++;
        }
        prep.executeQuery();
        prep.close();
    }

    public void createIndex(String name_, String table_, boolean unique_, String... columns_) throws SQLException {
        Connection con = connect();
        try {
            createIndex(con, name_, table_, unique_, columns_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    }

    public void dropIndex(Connection con_, String name_) throws SQLException {
        executeStatement(con_, "DROP INDEX " + name_ + " ;");
    }

    public void dropIndex(String name_) throws SQLException {
        executeStatement("DROP INDEX " + name_ + " ;");
    }

//===== 単項取得系メソッド ======================================================

    public long getLongByString(Connection con_, String table_, String keycolumn_, String key_, String valcolumn_) throws SQLException {
        PreparedStatement prep = null;
        ResultSet rs = null;
        long result = 0;
        prep = con_.prepareStatement("SELECT " + valcolumn_ + " FROM " + table_ + " WHERE " + keycolumn_ + " = ?");
        prep.setString(1, key_);
        rs = prep.executeQuery();
        // 結果無しならfalse復帰
        if (rs.next()) {
            result = rs.getLong(valcolumn_);
            rs.close();
            prep.close();
        } else {
            rs.close();
            prep.close();
            throw new SQLException("指定した値が見つかりませんでした");
        }
        return result;
    } 

    public long getLongByString(String table_, String keycolumn_, String key_, String valcolumn_) throws SQLException {
        Connection con = connect();
        long result = 0;
        try {
            result = getLongByString(con, table_, keycolumn_, key_, valcolumn_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
        return result;
    } 

    public long getLongByLong(Connection con_, String table_, String keycolumn_, long key_, String valcolumn_) throws SQLException {
        PreparedStatement prep = null;
        ResultSet rs = null;
        long result = 0;
        prep = con_.prepareStatement("SELECT " + valcolumn_ + " FROM " + table_ + " WHERE " + keycolumn_ + " = ?");
        prep.setLong(1, key_);
        rs = prep.executeQuery();
        // 結果無しならfalse復帰
        if (rs.next()) {
            result = rs.getLong(valcolumn_);
            rs.close();
            prep.close();
        } else {
            rs.close();
            prep.close();
            throw new SQLException("指定した値が見つかりませんでした");
        }
        return result;
    } 

    public long getLongByLong(String table_, String keycolumn_, long key_, String valcolumn_) throws SQLException {
        Connection con = connect();
        long result = 0;
        try {
            result = getLongByLong(con, table_, keycolumn_, key_, valcolumn_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
        return result;
    } 

    public float getFloatByString(Connection con_, String table_, String keycolumn_, String key_, String valcolumn_) throws SQLException {
        PreparedStatement prep = null;
        ResultSet rs = null;
        float result = 0;
        prep = con_.prepareStatement("SELECT " + valcolumn_ + " FROM " + table_ + " WHERE " + keycolumn_ + " = ?");
        prep.setString(1, key_);
        rs = prep.executeQuery();
        // 結果無しならfalse復帰
        if (rs.next()) {
            result = rs.getFloat(valcolumn_);
            rs.close();
            prep.close();
        } else {
            rs.close();
            prep.close();
            throw new SQLException("指定した値が見つかりませんでした");
        }
        return result;
    } 

    public float getFloatByString(String table_, String keycolumn_, String key_, String valcolumn_) throws SQLException {
        Connection con = connect();
        float result = 0;
        try {
            result = getFloatByString(con, table_, keycolumn_, key_, valcolumn_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
        return result;
    } 

    public float getFloatByLong(Connection con_, String table_, String keycolumn_, long key_, String valcolumn_) throws SQLException {
        PreparedStatement prep = null;
        ResultSet rs = null;
        float result = 0;
        prep = con_.prepareStatement("SELECT " + valcolumn_ + " FROM " + table_ + " WHERE " + keycolumn_ + " = ?");
        prep.setLong(1, key_);
        rs = prep.executeQuery();
        // 結果無しならfalse復帰
        if (rs.next()) {
            result = rs.getFloat(valcolumn_);
            rs.close();
            prep.close();
        } else {
            rs.close();
            prep.close();
            throw new SQLException("指定した値が見つかりませんでした");
        }
        return result;
    } 

    public float getFloatByLong(String table_, String keycolumn_, long key_, String valcolumn_) throws SQLException {
        Connection con = connect();
        float result = 0;
        try {
            result = getFloatByLong(con, table_, keycolumn_, key_, valcolumn_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
        return result;
    } 

    public String getStringByString(Connection con_, String table_, String keycolumn_, String key_, String valcolumn_) throws SQLException {
        PreparedStatement prep = null;
        ResultSet rs = null;
        String result = null;
        prep = con_.prepareStatement("SELECT " + valcolumn_ + " FROM " + table_ + " WHERE " + keycolumn_ + " = ?");
        prep.setString(1, key_);
        rs = prep.executeQuery();
        // 結果無しならfalse復帰
        if (rs.next()) {
            result = rs.getString(valcolumn_);
            rs.close();
            prep.close();
        } else {
            rs.close();
            prep.close();
            throw new SQLException("指定した値が見つかりませんでした");
        }
        return result;
    } 

    public String getStringByString(String table_, String keycolumn_, String key_, String valcolumn_) throws SQLException {
        Connection con = connect();
        String result = null;
        try {
            result = getStringByString(con, table_, keycolumn_, key_, valcolumn_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
        return result;
    } 

    public String getStringByLong(Connection con_, String table_, String keycolumn_, long key_, String valcolumn_) throws SQLException {
        PreparedStatement prep = null;
        ResultSet rs = null;
        String result = null;
        prep = con_.prepareStatement("SELECT " + valcolumn_ + " FROM " + table_ + " WHERE " + keycolumn_ + " = ?");
        prep.setLong(1, key_);
        rs = prep.executeQuery();
        // 結果無しならfalse復帰
        if (rs.next()) {
            result = rs.getString(valcolumn_);
            rs.close();
            prep.close();
        } else {
            rs.close();
            prep.close();
            throw new SQLException("指定した値が見つかりませんでした");
        }
        return result;
    } 

    public String getStringByLong(String table_, String keycolumn_, long key_, String valcolumn_) throws SQLException {
        Connection con = connect();
        String result = null;
        try {
            result = getStringByLong(con, table_, keycolumn_, key_, valcolumn_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
        return result;
    } 

    public byte[] getBlobByString(Connection con_, String table_, String keycolumn_, String key_, String valcolumn_) throws SQLException, IOException {
        PreparedStatement prep = null;
        ResultSet rs = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte [] result = new byte[1024];
        prep = con_.prepareStatement("SELECT " + valcolumn_ + " FROM " + table_ + " WHERE " + keycolumn_ + " = ?");
        prep.setString(1, key_);
        rs = prep.executeQuery();
        // 結果無しならfalse復帰
        if (rs.next()) {
//            InputStream in = rs.getBinaryStream(valcolumn_);
//            while (true) {
//                int bytes = in.read(result);
//                if (bytes == -1) {
//                    break;
//                }
//                baos.write(result,0,bytes);
//            }
//            result = baos.toByteArray();
            result = rs.getBytes(valcolumn_);

            rs.close();
            prep.close();
        } else {
            rs.close();
            prep.close();
            throw new SQLException("指定した値が見つかりませんでした");
        }
        return result;
    }

    public byte[] getBlobByString(String table_, String keycolumn_, String key_, String valcolumn_) throws SQLException, IOException {
        Connection con = connect();
        byte [] result = new byte[1024];
        try {
            result = getBlobByString(con, table_, keycolumn_, key_, valcolumn_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
        return result;
    }

    public byte[] getBlobByLong(Connection con_, String table_, String keycolumn_, long key_, String valcolumn_) throws SQLException, IOException {
        PreparedStatement prep = null;
        ResultSet rs = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte [] result = new byte[1024];
        prep = con_.prepareStatement("SELECT " + valcolumn_ + " FROM " + table_ + " WHERE " + keycolumn_ + " = ?");
        prep.setLong(1, key_);
        rs = prep.executeQuery();
        // 結果無しならfalse復帰
        if (rs.next()) {
//            InputStream in = rs.getb.getBinaryStream(valcolumn_);
//            while (true) {
//                int bytes = in.read(result);
//                if (bytes == -1) {
//                    break;
//                }
//                baos.write(result,0,bytes);
//            }
//            result = baos.toByteArray()
            result = rs.getBytes(valcolumn_);

            rs.close();
            prep.close();
        } else {
            rs.close();
            prep.close();
            throw new SQLException("指定した値が見つかりませんでした");
        }
        return result;
    } 

    public byte[] getBlobByLong(String table_, String keycolumn_, long key_, String valcolumn_) throws SQLException, IOException {
        Connection con = connect();
        byte [] result = new byte[1024];
        try {
            result = getBlobByLong(con, table_, keycolumn_, key_, valcolumn_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
        return result;
    } 

//===== 単項設定系メソッド ======================================================
    
    public void updateLongByString(Connection con_, String table_, String keycolumn_, String key_, String valcolumn_, long value_) throws SQLException {
        PreparedStatement prep = null;
        prep = con_.prepareStatement("UPDATE " + table_ + " SET " + valcolumn_ + " = ? WHERE " + keycolumn_ + " = ?");
        prep.setLong(1, value_);
        prep.setString(2, key_);
        prep.executeUpdate();
        prep.close();
    } 
    public void updateLongByString(String table_, String keycolumn_, String key_, String valcolumn_, long value_) throws SQLException {
        Connection con = connect();
        try {
            updateLongByString(con, table_, keycolumn_, key_, valcolumn_, value_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    } 
    public void updateLongByLong(Connection con_, String table_, String keycolumn_, long key_, String valcolumn_, long value_) throws SQLException {
        PreparedStatement prep = null;
        prep = con_.prepareStatement("UPDATE " + table_ + " SET " + valcolumn_ + " = ? WHERE " + keycolumn_ + " = ?");
        prep.setLong(1, value_);
        prep.setLong(2, key_);
        prep.executeUpdate();
        prep.close();
    } 
    public void updateLongByLong(String table_, String keycolumn_, long key_, String valcolumn_, long value_) throws SQLException {
        Connection con = connect();
        try {
            updateLongByLong(con, table_, keycolumn_, key_, valcolumn_, value_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    } 
    public void updateFloatByString(Connection con_, String table_, String keycolumn_, String key_, String valcolumn_, float value_) throws SQLException {
        PreparedStatement prep = null;
        prep = con_.prepareStatement("UPDATE " + table_ + " SET " + valcolumn_ + " = ? WHERE " + keycolumn_ + " = ?");
        prep.setFloat(1, value_);
        prep.setString(2, key_);
        prep.executeUpdate();
        prep.close();
    } 
    public void updateFloatByString(String table_, String keycolumn_, String key_, String valcolumn_, float value_) throws SQLException {
        Connection con = connect();
        try {
            updateFloatByString(con, table_, keycolumn_, key_, valcolumn_, value_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    } 
    public void updateFloatByLong(Connection con_, String table_, String keycolumn_, long key_, String valcolumn_, float value_) throws SQLException {
        PreparedStatement prep = null;
        prep = con_.prepareStatement("UPDATE " + table_ + " SET " + valcolumn_ + " = ? WHERE " + keycolumn_ + " = ?");
        prep.setFloat(1, value_);
        prep.setLong(2, key_);
        prep.executeUpdate();
        prep.close();
    } 
    public void updateFloatByLong(String table_, String keycolumn_, long key_, String valcolumn_, float value_) throws SQLException {
        Connection con = connect();
        try {
            updateFloatByLong(con, table_, keycolumn_, key_, valcolumn_, value_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    } 
    public void updateStringByString(Connection con_, String table_, String keycolumn_, String key_, String valcolumn_, String value_) throws SQLException {
        PreparedStatement prep = null;
        prep = con_.prepareStatement("UPDATE " + table_ + " SET " + valcolumn_ + " = ? WHERE " + keycolumn_ + " = ?");
        prep.setString(1, value_);
        prep.setString(2, key_);
        prep.executeUpdate();
        prep.close();
    } 
    public void updateStringByString(String table_, String keycolumn_, String key_, String valcolumn_, String value_) throws SQLException {
        Connection con = connect();
        try {
            updateStringByString(con, table_, keycolumn_, key_, valcolumn_, value_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    } 
    public void updateStringByLong(Connection con_, String table_, String keycolumn_, long key_, String valcolumn_, String value_) throws SQLException {
        PreparedStatement prep = null;
        prep = con_.prepareStatement("UPDATE " + table_ + " SET " + valcolumn_ + " = ? WHERE " + keycolumn_ + " = ?");
        prep.setString(1, value_);
        prep.setLong(2, key_);
        prep.executeUpdate();
        prep.close();
    } 
    public void updateStringByLong(String table_, String keycolumn_, long key_, String valcolumn_, String value_) throws SQLException {
        Connection con = connect();
        try {
            updateStringByLong(con, table_, keycolumn_, key_, valcolumn_, value_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    } 
    public void updateBlobByString(Connection con_, String table_, String keycolumn_, String key_, String valcolumn_, byte[] value_) throws SQLException, IOException {
        PreparedStatement prep = null;
        prep = con_.prepareStatement("UPDATE " + table_ + " SET " + valcolumn_ + " = ? WHERE " + keycolumn_ + " = ?");
        prep.setBytes(1, value_);
        prep.setString(2, key_);
        prep.executeUpdate();
        prep.close();
    } 
    public void updateBlobByString(String table_, String keycolumn_, String key_, String valcolumn_, byte[] value_) throws SQLException, IOException {
        Connection con = connect();
        try {
            updateBlobByString(con, table_, keycolumn_, key_, valcolumn_, value_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    } 
    public void updateBlobByLong(Connection con_, String table_, String keycolumn_, long key_, String valcolumn_, byte[] value_) throws SQLException, IOException {
        PreparedStatement prep = null;
        prep = con_.prepareStatement("UPDATE " + table_ + " SET " + valcolumn_ + " = ? WHERE " + keycolumn_ + " = ?");
        prep.setBytes(1, value_);
        prep.setLong(2, key_);
        prep.executeUpdate();
        prep.close();
    } 
    public void updateBlobByLong(String table_, String keycolumn_, long key_, String valcolumn_, byte[] value_) throws SQLException, IOException {
        Connection con = connect();
        try {
            updateBlobByLong(con, table_, keycolumn_, key_, valcolumn_, value_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    } 

//===== 単項挿入系メソッド ======================================================
    
    public void insertLongByString(Connection con_, String table_, String key_, long value_) throws SQLException {
        PreparedStatement prep = null;
        prep = con_.prepareStatement("INSERT INTO " + table_ + " VALUES(?, ?)");
        prep.setString(1, key_);
        prep.setLong(2, value_);
        prep.executeUpdate();
        prep.close();
    } 
    public void insertLongByString(String table_, String key_, long value_) throws SQLException {
        Connection con = connect();
        try {
            insertLongByString(con, table_, key_, value_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    } 
    public void insertLongByLong(Connection con_, String table_, long key_, long value_) throws SQLException {
        PreparedStatement prep = null;
        prep = con_.prepareStatement("INSERT INTO " + table_ + " VALUES(?, ?)");
        prep.setLong(1, key_);
        prep.setLong(2, value_);
        prep.executeUpdate();
        prep.close();
    } 
    public void insertLongByLong(String table_, long key_, long value_) throws SQLException {
        Connection con = connect();
        try {
            insertLongByLong(con, table_, key_, value_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    } 
    public void insertFloatByString(Connection con_, String table_, String key_, float value_) throws SQLException {
        PreparedStatement prep = null;
        prep = con_.prepareStatement("INSERT INTO " + table_ + " VALUES(?, ?)");
        prep.setString(1, key_);
        prep.setFloat(2, value_);
        prep.executeUpdate();
        prep.close();
    } 
    public void insertFloatByString(String table_, String key_, float value_) throws SQLException {
        Connection con = connect();
        try {
            insertFloatByString(con, table_, key_, value_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    } 
    public void insertFloatByLong(Connection con_, String table_, long key_, float value_) throws SQLException {
        PreparedStatement prep = null;
        prep = con_.prepareStatement("INSERT INTO " + table_ + " VALUES(?, ?)");
        prep.setLong(1, key_);
        prep.setFloat(2, value_);
        prep.executeUpdate();
        prep.close();
    } 
    public void insertFloatByLong(String table_, long key_, float value_) throws SQLException {
        Connection con = connect();
        try {
            insertFloatByLong(con, table_, key_, value_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    } 
    public void insertStringByString(Connection con_, String table_, String key_, String value_) throws SQLException {
        PreparedStatement prep = null;
        prep = con_.prepareStatement("INSERT INTO " + table_ + " VALUES(?, ?)");
        prep.setString(1, key_);
        prep.setString(2, value_);
        prep.executeUpdate();
        prep.close();
    } 
    public void insertStringByString(String table_, String key_, String value_) throws SQLException {
        Connection con = connect();
        try {
            insertStringByString(con, table_, key_, value_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    } 
    public void insertStringByLong(Connection con_, String table_, long key_, String value_) throws SQLException {
        PreparedStatement prep = null;
        prep = con_.prepareStatement("INSERT INTO " + table_ + " VALUES(?, ?)");
        prep.setLong(1, key_);
        prep.setString(2, value_);
        prep.executeUpdate();
        prep.close();
    } 
    public void insertStringByLong(String table_, long key_, String value_) throws SQLException {
        Connection con = connect();
        try {
            insertStringByLong(con, table_, key_, value_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    } 
    public void insertBlobByString(Connection con_, String table_, String key_, byte[] value_) throws SQLException, IOException {
        PreparedStatement prep = null;
        prep = con_.prepareStatement("INSERT INTO " + table_ + " VALUES(?, ?)");
        prep.setString(1, key_);
        prep.setBytes(2, value_);
        prep.executeUpdate();
        prep.close();
    } 
    public void insertBlobByString(String table_, String key_, byte[] value_) throws SQLException, IOException {
        Connection con = connect();
        try {
            insertBlobByString(con, table_, key_, value_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    } 
    public void insertBlobByLong(Connection con_, String table_, long key_, byte[] value_) throws SQLException, IOException {
        PreparedStatement prep = null;
        prep = con_.prepareStatement("INSERT INTO " + table_ + " VALUES(?, ?)");
        prep.setLong(1, key_);
        prep.setBytes(2, value_);
        prep.executeUpdate();
        prep.close();
    } 
    public void insertBlobByLong(String table_, long key_, byte[] value_) throws SQLException, IOException {
        Connection con = connect();
        try {
            insertBlobByLong(con, table_, key_, value_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    } 

//===== カラム指定単項挿入系メソッド ======================================================
    
    public void insertLongByString(Connection con_, String table_, String keycolumn_, String key_, String valcolumn_, long value_) throws SQLException {
        PreparedStatement prep = null;
        prep = con_.prepareStatement("INSERT INTO " + table_ + "(" + keycolumn_ + ", " + valcolumn_ + ") VALUES(?, ?)");
        prep.setString(1, key_);
        prep.setLong(2, value_);
        prep.executeUpdate();
        prep.close();
    } 
    public void insertLongByString(String table_, String keycolumn_, String key_, String valcolumn_, long value_) throws SQLException {
        Connection con = connect();
        try {
            insertLongByString(con, table_, keycolumn_, key_, valcolumn_, value_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    } 
    public void insertLongByLong(Connection con_, String table_, String keycolumn_, long key_, String valcolumn_, long value_) throws SQLException {
        PreparedStatement prep = null;
        prep = con_.prepareStatement("INSERT INTO " + table_ + "(" + keycolumn_ + ", " + valcolumn_ + ") VALUES(?, ?)");
        prep.setLong(1, key_);
        prep.setLong(2, value_);
        prep.executeUpdate();
        prep.close();
    } 
    public void insertLongByLong(String table_, String keycolumn_, long key_, String valcolumn_, long value_) throws SQLException {
        Connection con = connect();
        try {
            insertLongByLong(con, table_, keycolumn_, key_, valcolumn_, value_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    } 
    public void insertFloatByString(Connection con_, String table_, String keycolumn_, String key_, String valcolumn_, float value_) throws SQLException {
        PreparedStatement prep = null;
        prep = con_.prepareStatement("INSERT INTO " + table_ + "(" + keycolumn_ + ", " + valcolumn_ + ") VALUES(?, ?)");
        prep.setString(1, key_);
        prep.setFloat(2, value_);
        prep.executeUpdate();
        prep.close();
    } 
    public void insertFloatByString(String table_, String keycolumn_, String key_, String valcolumn_, float value_) throws SQLException {
        Connection con = connect();
        try {
            insertFloatByString(con, table_, keycolumn_, key_, valcolumn_, value_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    } 
    public void insertFloatByLong(Connection con_, String table_, String keycolumn_, long key_, String valcolumn_, float value_) throws SQLException {
        PreparedStatement prep = null;
        prep = con_.prepareStatement("INSERT INTO " + table_ + "(" + keycolumn_ + ", " + valcolumn_ + ") VALUES(?, ?)");
        prep.setLong(1, key_);
        prep.setFloat(2, value_);
        prep.executeUpdate();
        prep.close();
    } 
    public void insertFloatByLong(String table_, String keycolumn_, long key_, String valcolumn_, float value_) throws SQLException {
        Connection con = connect();
        try {
            insertFloatByLong(con, table_, keycolumn_, key_, valcolumn_, value_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    }
    public void insertStringByString(Connection con_, String table_, String keycolumn_, String key_, String valcolumn_, String value_) throws SQLException {
        PreparedStatement prep = null;
        prep = con_.prepareStatement("INSERT INTO " + table_ + "(" + keycolumn_ + ", " + valcolumn_ + ") VALUES(?, ?)");
        prep.setString(1, key_);
        prep.setString(2, value_);
        prep.executeUpdate();
        prep.close();
    } 
    public void insertStringByString(String table_, String keycolumn_, String key_, String valcolumn_, String value_) throws SQLException {
        Connection con = connect();
        try {
            insertStringByString(con, table_, keycolumn_, key_, valcolumn_, value_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    } 
    public void insertStringByLong(Connection con_, String table_, String keycolumn_, long key_, String valcolumn_, String value_) throws SQLException {
        PreparedStatement prep = null;
        prep = con_.prepareStatement("INSERT INTO " + table_ + "(" + keycolumn_ + ", " + valcolumn_ + ") VALUES(?, ?)");
        prep.setLong(1, key_);
        prep.setString(2, value_);
        prep.executeUpdate();
        prep.close();
    } 
    public void insertStringByLong(String table_, String keycolumn_, long key_, String valcolumn_, String value_) throws SQLException {
        Connection con = connect();
        try {
            insertStringByLong(con, table_, keycolumn_, key_, valcolumn_, value_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    } 
    public void insertBlobByString(Connection con_, String table_, String keycolumn_, String key_, String valcolumn_, byte[] value_) throws SQLException, IOException {
        PreparedStatement prep = null;
        prep = con_.prepareStatement("INSERT INTO " + table_ + "(" + keycolumn_ + ", " + valcolumn_ + ") VALUES(?, ?)");
        prep.setString(1, key_);
        prep.setBytes(2, value_);
        prep.executeUpdate();
        prep.close();
    } 
    public void insertBlobByString(String table_, String keycolumn_, String key_, String valcolumn_, byte[] value_) throws SQLException, IOException {
        Connection con = connect();
        try {
            insertBlobByString(con, table_, keycolumn_, key_, valcolumn_, value_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    } 
    public void insertBlobByLong(Connection con_, String table_, String keycolumn_, long key_, String valcolumn_, byte[] value_) throws SQLException, IOException {
        PreparedStatement prep = null;
        prep = con_.prepareStatement("INSERT INTO " + table_ + "(" + keycolumn_ + ", " + valcolumn_ + ") VALUES(?, ?)");
        prep.setLong(1, key_);
        prep.setBytes(2, value_);
        prep.executeUpdate();
        prep.close();
    } 
    public void insertBlobByLong(String table_, String keycolumn_, long key_, String valcolumn_, byte[] value_) throws SQLException, IOException {
        Connection con = connect();
        try {
            insertBlobByLong(con, table_, keycolumn_, key_, valcolumn_, value_);
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
    } 

//===== Framework共通設定系メソッド ==============================================

    private static final String PREFIX_FRAMEWORK = "ecoframe_";
    private static final String TABLE_SETTING = PREFIX_FRAMEWORK+"settings";
    public boolean isExistSettings() throws SQLException {
        return constans(TABLE_SETTING);
    }
    
    public void createSettings(Connection con_) throws SQLException {
        log.info("create framework settings table.");
        // 絶対重いだろこの処理…
        MessageFormat mf = new MessageFormat("CREATE TABLE {0} ( name {1}, snum {2}, fnum {3}, str {4}, data {5}, PRIMARY KEY {6})");
        executeStatement(con_, mf.format(new String[]{ TABLE_SETTING, CTYPE.STRING.get(jdbc), CTYPE.LONG.get(jdbc), CTYPE.FLOAT.get(jdbc), CTYPE.STRING.get(jdbc), CTYPE.BLOB.get(jdbc), CTYPE.STRING.primary(jdbc, "name")}));
//        executeStatement("CREATE TABLE " + TABLE_SETTING + "( name TEXT PRIMARY KEY(128), snum INTEGER, fnum REAL, str TEXT, data BLOB )");
        log.info("insert framework settings default value.");
        insertLongSettings(con_, "version", 1L);
    }
    public void createSettings() throws SQLException {
        log.info("create framework settings table.");
        // 絶対重いだろこの処理…
        MessageFormat mf = new MessageFormat("CREATE TABLE {0} ( name {1}, snum {2}, fnum {3}, str {4}, data {5}, PRIMARY KEY {6})");
        executeStatement(mf.format(new String[]{ TABLE_SETTING, CTYPE.STRING.get(jdbc), CTYPE.LONG.get(jdbc), CTYPE.FLOAT.get(jdbc), CTYPE.STRING.get(jdbc), CTYPE.BLOB.get(jdbc), CTYPE.STRING.primary(jdbc, "name")}));
//        executeStatement("CREATE TABLE " + TABLE_SETTING + "( name TEXT PRIMARY KEY(128), snum INTEGER, fnum REAL, str TEXT, data BLOB )");
        log.info("insert framework settings default value.");
        insertLongSettings("version", 1L);
    }

    public void insertLongSettings(Connection con_, String name_, Long snum_) throws SQLException {
        insertLongByString(con_, TABLE_SETTING, "name", name_, "snum", snum_);
    }
    public void insertLongSettings(String name_, Long snum_) throws SQLException {
        insertLongByString(TABLE_SETTING, "name", name_, "snum", snum_);
    }

    public void insertFloatSettings(Connection con_, String name_, float fnum_) throws SQLException {
        insertFloatByString(con_, TABLE_SETTING, "name", name_, "fnum", fnum_);
    }
    public void insertFloatSettings(String name_, float fnum_) throws SQLException {
        insertFloatByString(TABLE_SETTING, "name", name_, "fnum", fnum_);
    }

    public void insertStringSettings(Connection con_, String name_, String str_) throws SQLException {
        insertStringByString(con_, TABLE_SETTING, "name", name_, "str", str_);
    }
    public void insertStringSettings(String name_, String str_) throws SQLException {
        insertStringByString(TABLE_SETTING, "name", name_, "str", str_);
    }

    public void insertBlobSettings(Connection con_, String name_, byte[] data_) throws SQLException, IOException {
        insertBlobByString(con_, TABLE_SETTING, "name", name_, "data", data_);
    }
    public void insertBlobSettings(String name_, byte[] data_) throws SQLException, IOException {
        insertBlobByString(TABLE_SETTING, "name", name_, "data", data_);
    }

    public long getLongSettings(Connection con_, String name_) throws SQLException {
        return getLongByString(con_, TABLE_SETTING, "name", name_, "snum");
    }
    public long getLongSettings(String name_) throws SQLException {
        return getLongByString(TABLE_SETTING, "name", name_, "snum");
    }

    public float getFloatSettings(Connection con_, String name_) throws SQLException {
        return getFloatByString(con_, TABLE_SETTING, "name", name_, "fnum");
    }
    public float getFloatSettings(String name_) throws SQLException {
        return getFloatByString(TABLE_SETTING, "name", name_, "fnum");
    }

    public String getStringSettings(Connection con_, String name_) throws SQLException {
        return getStringByString(con_, TABLE_SETTING, "name", name_, "str");
    }
    public String getStringSettings(String name_) throws SQLException {
        return getStringByString(TABLE_SETTING, "name", name_, "str");
    }

    public byte[] getBlobSettings(Connection con_, String name_) throws SQLException, IOException {
        return getBlobByString(con_, TABLE_SETTING, "name", name_, "data");
    }
    public byte[] getBlobSettings(String name_) throws SQLException, IOException {
        return getBlobByString(TABLE_SETTING, "name", name_, "data");
    }

    public long getSettingsVersion(Connection con_) throws SQLException {
        return getLongByString(con_, TABLE_SETTING, "name", "version", "snum");
    }
    public long getSettingsVersion() throws SQLException {
        return getLongByString(TABLE_SETTING, "name", "version", "snum");
    }

    public void updateSettingsVersion(Connection con_, long version_) throws SQLException {
        updateLongByString(con_, TABLE_SETTING, "name", "version", "snum", version_);
    }
    public void updateSettingsVersion(long version_) throws SQLException {
        updateLongByString(TABLE_SETTING, "name", "version", "snum", version_);
    }

    public void updateSettingsVersion(Connection con_) throws SQLException {
        updateSettingsVersion(con_, getSettingsVersion(con_)+ 1);
        dbversion = getSettingsVersion(con_);
    }
    public void updateSettingsVersion() throws SQLException {
        updateSettingsVersion(getSettingsVersion() + 1);
        dbversion = getSettingsVersion();
    }

}

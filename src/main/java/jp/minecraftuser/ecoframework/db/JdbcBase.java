
package jp.minecraftuser.ecoframework.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import jp.minecraftuser.ecoframework.ConfigFrame;
import jp.minecraftuser.ecoframework.PluginFrame;

/**
 *
 * @author ecolight
 */
public abstract class JdbcBase {
    private boolean owner;
    private Properties p = null;
    protected final String dbname;
    protected final PluginFrame plg;
    protected final Logger log;
    protected final ConfigFrame conf;
    protected boolean justCreated = false;
    protected Connection con;

    public JdbcBase(PluginFrame plg_, String dbname_) throws ClassNotFoundException, SQLException {
        plg = plg_;
        log = plg.getLogger();
        conf = plg.getDefaultConfig();
        dbname = dbname_;
    }

    public Connection connect() throws ClassNotFoundException, SQLException {
        log.info("Start connecting database.[" + dbname + "]");
        // 継承クラスで事前準備がある場合には処理させる
        preConnection();
        
        // 使用するJDBCドライバの登録、使用クラスは継承クラスに定義してもらう
        Class.forName(setDriver()); // static initializer でJDBCドライバを登録する

        // 継承クラスのプロパティ設定をコールする
        registerProperty();
        
        // DB接続
        if (p == null) {
            con = DriverManager.getConnection(connectDB());
        } else {
            con = DriverManager.getConnection(connectDB(), p);
        }
        con.setAutoCommit(false); // 悩ましいが必ずBEGIN, commitする事にする
        owner = true;

        // デフォルトではSERIALIZABLE指定とする。共用DBでパフォーマンスの検討をする場合は変更してから使用すること
        setTransactionIsolationSerializable();
        log.info("database settings complete.[" + dbname + "]");

        // 継承クラスでDB接続の後処理がある場合にはコールする
        afterConnection();
        
        return con;
    }

    /**
     * DB接続前処理
     */
    protected void preConnection() {
        // optional method
    }
    
    /**
     * DB接続後処理
     */
    protected void afterConnection() throws SQLException {
        // optional method
    }

    /**
     * Jdbcドライバのクラス指定
     * @return JDBCドライバクラス名
     */
    protected abstract String setDriver();
    
    /**
     * プロパティ追加処理
     * @param key キー
     * @param value 値
     */
    protected void setProperty(String key, String value) {
        if (p == null) {
            p = new Properties();
        }
        p.setProperty(key, value);
    }

    /**
     * プロパティ設定処理
     */
    protected void registerProperty() {
        // optional method
    }

    /**
     * DB接続のためのコネクション文字列指定
     * @return DB接続指示文字列
     */
    protected abstract String connectDB();
    
    /**
     * データベースインスタンス破棄処理
     * 
     * 本クラスをインスタンス化した場合破棄するタイミングで必ず呼ぶこと。finalizeには頼らない。
     * 自分が生成したDBのみ処理する
     */
    public void close() {
        if (owner) {
            try {
                con.close();
            } catch (SQLException ex) {
                log.warning("["+dbname+"] database のクローズ処理に失敗しました");
                log.log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /**
     * DBが作成直後かどうかを返却する
     * @return trueの場合作成直後
     */
    public boolean isJustCreated() {
        return justCreated;
    }
    
//===== DB基本操作ユーティリティ =================================================

    /**
     * 分離レベルを排他無しに設定する
     * 呼び出し元は他操作を行う前に必要な分離レベルに設定を変えることができる。
     * NONE:トランザクション管理しない。どう動くんだこれ。
     * @throws SQLException
     */
    public final void setTransactionIsolationNone() throws SQLException {
        con.setTransactionIsolation(Connection.TRANSACTION_NONE);
    }
    
    /**
     * 分離レベルをREAD UNCOMMITTEDに設定する
     * 呼び出し元は他操作を行う前に必要な分離レベルに設定を変えることができる。
     * READ_UNCOMMITTED:他トランザクションのコミット前の変更データが見える。Rollback何か起きたら整合性壊れる。
     * ダーティリード、ファジーリード、ファントムリードが発生しうる。
     * @throws SQLException
     */
    public final void setTransactionIsolationUncommitted() throws SQLException {
        con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
    }
    
    /**
     * 分離レベルをREAD COMMITTEDに設定する
     * 呼び出し元は他操作を行う前に必要な分離レベルに設定を変えることができる。
     * READ_COMMITTED:他トランザクションの更新後データが読み取れる。
     * ファジーリード、ファントムリードが発生しうる。
     * @throws SQLException
     */
    public final void setTransactionIsolationCommitted() throws SQLException {
        con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
    }

    /**
     * 分離レベルをREPEATABLE READに設定する
     * 呼び出し元は他操作を行う前に必要な分離レベルに設定を変えることができる。
     * REPEATABLE_READ:他トランザクションの挿入データが読み取れる。
     * ファントムリードが発生しうる。MySQLだと発生しないらしい。
     * @throws SQLException
     */
    public final void setTransactionIsolationRepeatable() throws SQLException {
        con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
    }

    /**
     * 分離レベルをSERIALIZABLEに設定する
     * 呼び出し元は他操作を行う前に必要な分離レベルに設定を変えることができる。
     * SERIALIZABLE:他トランザクションとは完全に隔離される。
     * 各種読み込み不具合は発生しない。
     * @throws SQLException
     */
    public final void setTransactionIsolationSerializable() throws SQLException {
        con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
    }

    /**
     * ステートメント実行処理
     * SQLインジェクション防止のためpreparedステートメント使用せずにユーザー入力文字列をコミットしないこと
     * @param sql_ 実行SQL文
     * @throws SQLException
     */
    public void executeStatement(String sql_) throws SQLException {
        Statement stmt = con.createStatement();
        stmt.executeUpdate(sql_);
        stmt.close();
    }
    
    /**
     * テーブルの存在チェック
     * @param table_ 検索テーブル名
     * @return 有無を示すboolean値
     * @throws SQLException 
     */
    public abstract boolean constans(String table_) throws SQLException;
    
    /**
     * カラムの存在チェック
     * @param table_
     * @param column_ カラム名
     * @return 有無を示すboolean値(テーブルもカラムもある場合のみtrue)
     * @throws SQLException 
     */
    public abstract boolean constans(String table_, String column_) throws SQLException;
}

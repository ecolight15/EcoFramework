
package jp.minecraftuser.ecoframework.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
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
    protected HikariDataSource hikari;
    protected String user;
    protected String pass;
    protected String addr;

    public JdbcBase(PluginFrame plg_, String dbname_) throws ClassNotFoundException, SQLException {
        plg = plg_;
        log = plg.getLogger();
        conf = plg.getDefaultConfig();
        dbname = dbname_;
        user = "";
        pass = "";
        addr = "";
        init();
    }

    public JdbcBase(PluginFrame plg_, String addr_, String user_, String pass_, String dbname_) throws ClassNotFoundException, SQLException {
        plg = plg_;
        log = plg.getLogger();
        conf = plg.getDefaultConfig();
        dbname = dbname_;
        user = user_;
        pass = pass_;
        addr = addr_;
        init();
    }

    private void init() throws SQLException {
        log.info("Start connecting database.[" + dbname + "]");
        // 継承クラスで事前準備がある場合には処理させる
        preConnection();
        
        // プロパティの取得
        registerProperty();
        
        // HikariCP初期化
        HikariConfig config = null;
        if (p == null) {
            config = new HikariConfig();
        } else {
            config = new HikariConfig(p);
        }
        
        // 使用するJDBCドライバの登録、使用クラスは継承クラスに定義してもらう
        config.setDriverClassName(setDriver()); // JDBCドライバを登録する
        
        // データベースパスの設定
        config.setJdbcUrl(connectDB());

        // JdbcDriver固有の設置をコールする
        registerConfig(config);

        // コネクションプーリングで投げるSQL
        config.setConnectionInitSql("SELECT 1");
        
        // DB接続
        hikari = new HikariDataSource(config);

        // 継承クラスでDB接続の後処理がある場合にはコールする
        afterConnection();
        
        log.info("database connection complete.[" + dbname + "]");

        return;
    }

    /**
     * コネクションの取得
     * @return コネクションプールから取得したコネクションを返却する
     * @throws ClassNotFoundException
     * @throws SQLException 
     */
    public Connection connect() throws SQLException  {
        log.info("Start connecting database.[" + dbname + "]");
        // DB接続
        Connection con = hikari.getConnection();
        try {
            con.setAutoCommit(false); // 悩ましいが必ずBEGIN, commitする事にする
            owner = true;

            // デフォルトではSERIALIZABLE指定とする。共用DBでパフォーマンスの検討をする場合は変更してから使用すること
            setTransactionIsolationSerializable(con);
        } catch (Exception e) {
            // コネクションの取得は出来ているので、ログだけしてコネクションを返す
            log.warning(e.getLocalizedMessage());
            e.printStackTrace();
        }
        log.info("database get connection complete.[" + dbname + "]");

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
     * コネクションプーリング設定処理
     * @param config HikariCP設定のインスタンスを指定する
     */
    protected void registerConfig(HikariConfig config) {
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
            if (hikari != null) {
                hikari.close();
                hikari = null;
            } else {
                log.warning("["+dbname+"] database が null なのでクローズをスキップしました");
            }
        } else {
            log.warning("["+dbname+"] database のオーナーではないためクローズをスキップしました");
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
// Connectionにのみ作用する処理はcloseした段階で無効になるかもしれないが未検証
// コネクションプーリングの対応によりここらへんの動作が不明瞭なため必要に応じて
// コネクションの取得後に独自に実施すること

    /**
     * 分離レベルを排他無しに設定する
     * 呼び出し元は他操作を行う前に必要な分離レベルに設定を変えることができる。
     * NONE:トランザクション管理しない。どう動くんだこれ。
     * @param con コネクションを渡す
     * @throws SQLException
     */
    public final void setTransactionIsolationNone(Connection con) throws SQLException {
        con.setTransactionIsolation(Connection.TRANSACTION_NONE);
    }
    
    /**
     * 分離レベルをREAD UNCOMMITTEDに設定する
     * 呼び出し元は他操作を行う前に必要な分離レベルに設定を変えることができる。
     * READ_UNCOMMITTED:他トランザクションのコミット前の変更データが見える。Rollback何か起きたら整合性壊れる。
     * ダーティリード、ファジーリード、ファントムリードが発生しうる。
     * @param con コネクションを渡す
     * @throws SQLException
     */
    public final void setTransactionIsolationUncommitted(Connection con) throws SQLException {
        con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
    }
    
    /**
     * 分離レベルをREAD COMMITTEDに設定する
     * 呼び出し元は他操作を行う前に必要な分離レベルに設定を変えることができる。
     * READ_COMMITTED:他トランザクションの更新後データが読み取れる。
     * ファジーリード、ファントムリードが発生しうる。
     * @param con コネクションを渡す
     * @throws SQLException
     */
    public final void setTransactionIsolationCommitted(Connection con) throws SQLException {
        con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
    }

    /**
     * 分離レベルをREPEATABLE READに設定する
     * 呼び出し元は他操作を行う前に必要な分離レベルに設定を変えることができる。
     * REPEATABLE_READ:他トランザクションの挿入データが読み取れる。
     * ファントムリードが発生しうる。MySQLだと発生しないらしい。
     * @param con コネクションを渡す
     * @throws SQLException
     */
    public final void setTransactionIsolationRepeatable(Connection con) throws SQLException {
        con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
    }

    /**
     * 分離レベルをSERIALIZABLEに設定する
     * 呼び出し元は他操作を行う前に必要な分離レベルに設定を変えることができる。
     * SERIALIZABLE:他トランザクションとは完全に隔離される。
     * 各種読み込み不具合は発生しない。
     * @param con コネクションを渡す
     * @throws SQLException
     */
    public final void setTransactionIsolationSerializable(Connection con) throws SQLException {
        con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
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
            Statement stmt = con.createStatement();
            stmt.executeUpdate(sql_);
            stmt.close();
            con.close();
        } catch (Exception e) {
            con.close();
            throw e;
        }
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

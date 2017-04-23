
package jp.minecraftuser.ecoframework.store;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.UUID;
import jp.minecraftuser.ecoframework.DatabaseFrame;
import jp.minecraftuser.ecoframework.PluginFrame;
import jp.minecraftuser.ecoframework.db.CTYPE;


/**
 * プレイヤー固有ファイル保存
 * @author ecolight
 */
public class PlayerFileStore extends DatabaseFrame {

    public PlayerFileStore(PluginFrame plg_, String dbfilepath_, String name_) throws ClassNotFoundException, SQLException {
        super(plg_, dbfilepath_, name_);
    }

    public PlayerFileStore(PluginFrame plg_, String server_, String user_, String pass_, String dbname_, String name_) throws ClassNotFoundException, SQLException {
        super(plg_, server_, user_, pass_, dbname_, name_);
    }

    /**
     * データベース移行処理
     * 基底クラスからDBをオープンするインスタンスの生成時に呼ばれる
     * 
     * @throws SQLException
     */
    @Override
    protected void migrationData() throws SQLException {
        // version 1 の場合、新規作成もしくは旧バージョンのデータベース引き継ぎの場合を検討する
        if (dbversion == 1) {
            if ((justCreated) || (!constans("datatable"))) {
                MessageFormat mf = new MessageFormat("CREATE TABLE IF NOT EXISTS datatable(most {0} NOT NULL, least {1} NOT NULL, logout {2} NOT NULL, name {3} NOT NULL, size {4} NOT NULL, data {5} NOT NULL, PRIMARY KEY(most, least))");
                executeStatement(mf.format(new String[]{CTYPE.LONG.get(jdbc), CTYPE.LONG.get(jdbc), CTYPE.LONG.get(jdbc), CTYPE.STRING.get(jdbc), CTYPE.LONG.get(jdbc), CTYPE.BLOB.get(jdbc)}));
                // 新規作成の場合、テーブル定義のみ作成して終わり
//                executeStatement("CREATE TABLE IF NOT EXISTS datatable(most INTEGER NOT NULL, least INTEGER NOT NULL, logout BOOLEAN NOT NULL, name TEXT NOT NULL, size INTEGER NOT NULL, data BLOB NOT NULL, PRIMARY KEY(most, least))");
                log.info(name + "DataBase checked.");
                // データベースバージョンは最新版数に設定する
                log.info("create " + name + " version 2");
                updateSettingsVersion(2);
                return;
            } else {
                // 既存DB引き継ぎの場合は新規作成と同レベルの状態にする必要がある
                // 1 -> 2版の変更内容
                // - バージョン 1 は既存DBがある場合のバージョン
                // - xxx追加, yyy削除
                log.info("convert " + name + " version 1 -> 2 start");
                
                // カラム追加
//                addLongColumn("table", "column", 1L);
                
                // xxxテーブルにを置き換え
//                executeStatement("CREATE TABLE IF NOT EXISTS buf(user INTEGER NOT NULL, memo TEXT NOT NULL, FOREIGN KEY(userid) REFERENCES users(userid) ON DELETE CASCADE");
//                dropTable("xxx");
//                renameTable("buf", "xxx");
                
                // データベースバージョンは次版にする
                updateSettingsVersion();
                
                log.info("convert " + name + " version 1 -> 2 complete");
            }
        }
        // Version 2 -> 3
//        if (dbversion == 2) {
//            log.info("convert " + name + " version " + dbversion + " -> " + (dbversion + 1) + " start");;
//            // optional
//            log.info("convert " + name + " version " + dbversion + " -> " + (dbversion + 1) + " complete");
//        }
    }

    /**
     * プレイヤー固有ファイル保存
     * @param uuid プレイヤーUUID
     * @param file 保存ファイル
     * @throws SQLException
     * @throws java.io.FileNotFoundException
     */
    public void savePlayerData(UUID uuid, File file) throws SQLException, FileNotFoundException, IOException {
        // SQLコンパイル
        PreparedStatement prep = con.prepareStatement("REPLACE INTO datatable VALUES (?, ?, ?, ?, ?, ?)");
        try {
            prep.setLong(1, uuid.getMostSignificantBits());
            prep.setLong(2, uuid.getLeastSignificantBits());
            prep.setBoolean(3, true);
            prep.setString(4, file.getName());
            prep.setLong(5, file.length());
            byte[] buf = new byte[(int)file.length()];
            FileInputStream fis = new FileInputStream(file);
            fis.read(buf, 0, (int) file.length());
            fis.close();
            prep.setBytes(6, buf);
            
            // 実行
            prep.executeUpdate();
        } catch (SQLException | FileNotFoundException ex) {
            // 抜けるため後処理
            prep.close();
            // 投げなおして上位で異常検知させる
            throw ex;
        }
        // 後処理
        prep.close();
    }

    /**
     * プレイヤー固有ファイル読み出し
     * @param uuid プレイヤーUUID
     * @param file ファイルパスを含むファイルインスタンス
     * @throws SQLException
     * @throws IOException
     */
    public void loadPlayerData(UUID uuid, File file) throws SQLException, IOException {
        // SQLコンパイル
        PreparedStatement prep = con.prepareStatement("SELECT * FROM datatable WHERE most = ? AND least = ?");
        try {
            prep.setLong(1, uuid.getMostSignificantBits());
            prep.setLong(2, uuid.getLeastSignificantBits());
            // 実行
            ResultSet rs = prep.executeQuery();
            file.delete();
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            try {
                // 結果取得
                if (rs.next()) {
                    fos.write(rs.getBytes("data"));
                }
            } catch (SQLException | IOException ex) {
                // PreparedStatementがcloseできればカーソルリークしないはずだが、
                // 念のため確実にResultSetをcloseするようにしておく
                fos.close();
                rs.close();
                // 投げなおして上位で異常検知させる
                throw ex;
            }
            // 後処理
            if (fos != null) {
                fos.close();
            }
            rs.close();
        } catch (SQLException | IOException ex) {
            prep.close();
            throw ex;
        }
        prep.close();
    }

    /**
     * プレイヤー固有ファイル照会
     * @param uuid プレイヤーUUID
     * @return レコード有無
     * @throws SQLException
     */
    public boolean existPlayerData(UUID uuid) throws SQLException {
        // SQLコンパイル
        boolean result = false;
        PreparedStatement prep = con.prepareStatement("SELECT COUNT(*) FROM datatable WHERE most = ? AND least = ?");
        try {
            prep.setLong(1, uuid.getMostSignificantBits());
            prep.setLong(2, uuid.getLeastSignificantBits());
            // 実行
            ResultSet rs = prep.executeQuery();
            try {
                // 結果取得
                if (rs.next()) {
                    if (rs.getLong(1) != 0) {
                        result = true;
                    }
                }
            } catch (SQLException ex) {
                // PreparedStatementがcloseできればカーソルリークしないはずだが、
                // 念のため確実にResultSetをcloseするようにしておく
                rs.close();
                // 投げなおして上位で異常検知させる
                throw ex;
            }
            // 後処理
            rs.close();
        } catch (SQLException ex) {
            prep.close();
            throw ex;
        }
        prep.close();
        return result;
    }

    /**
     * プレイヤー固有ファイル更新(ログアウト開始)
     * @param uuid プレイヤーUUID
     * @throws SQLException
     */
    public void logoutPlayer(UUID uuid) throws SQLException {
        // SQLコンパイル
        PreparedStatement prep = con.prepareStatement("UPDATE datatable SET logout = ? WHERE most = ? AND least = ?");
        try {
            prep.setBoolean(1, false);
            prep.setLong(2, uuid.getMostSignificantBits());
            prep.setLong(3, uuid.getLeastSignificantBits());
            
            // 実行
            prep.executeUpdate();
        } catch (SQLException ex) {
            // 抜けるため後処理
            prep.close();
            // 投げなおして上位で異常検知させる
            throw ex;
        }
        // 後処理
        prep.close();
    }
    
    /**
     * プレイヤーログアウト処理状況照会
     * @param uuid プレイヤーUUID
     * @return ログアウト済みかどうか false:ログアウト処理中 true:ログアウト済み
     * @throws SQLException
     */
    public boolean isPlayerAfterLogout(UUID uuid) throws SQLException {
        // SQLコンパイル
        boolean result = true;
        PreparedStatement prep = con.prepareStatement("SELECT logout FROM datatable WHERE most = ? AND least = ?");
        try {
            prep.setLong(1, uuid.getMostSignificantBits());
            prep.setLong(2, uuid.getLeastSignificantBits());
            // 実行
            ResultSet rs = prep.executeQuery();
            try {
                // 結果取得
                if (rs.next()) {
                    result = rs.getBoolean("logout");
                }
            } catch (SQLException ex) {
                // PreparedStatementがcloseできればカーソルリークしないはずだが、
                // 念のため確実にResultSetをcloseするようにしておく
                rs.close();
                // 投げなおして上位で異常検知させる
                throw ex;
            }
            // 後処理
            rs.close();
        } catch (SQLException ex) {
            prep.close();
            throw ex;
        }
        prep.close();
        return result;
    }
}

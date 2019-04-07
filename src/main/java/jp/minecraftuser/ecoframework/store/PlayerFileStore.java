
package jp.minecraftuser.ecoframework.store;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.UUID;
import java.util.logging.Level;
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
    protected void migrationData(Connection con) throws SQLException {
        // 全体的にテーブル操作になるため、暗黙的コミットが走り失敗してもロールバックが効かない
        // 十分なテストの後にリリースするか、何らかの形で異常検知し、DBバージョンに従い元に戻せるようテーブル操作順を考慮する必要がある
        // 本処理においては取り敢えずロールバックは諦める
        
        // version 1 の場合、新規作成もしくは旧バージョンのデータベース引き継ぎの場合を検討する
        if (dbversion == 1) {
            if ((justCreated) || (!constans("datatable"))) {
                // 新規作成の場合、初版のテーブルのみ作成して終わり
                MessageFormat mf = new MessageFormat("CREATE TABLE IF NOT EXISTS datatable(most {0} NOT NULL, least {1} NOT NULL, logout {2} NOT NULL, name {3} NOT NULL, size {4} NOT NULL, data {5} NOT NULL, PRIMARY KEY(most, least))");
                executeStatement(mf.format(new String[]{CTYPE.LONG.get(jdbc), CTYPE.LONG.get(jdbc), CTYPE.LONG.get(jdbc), CTYPE.STRING.get(jdbc), CTYPE.LONG.get(jdbc), CTYPE.BLOB.get(jdbc)}));
                log.log(Level.INFO, "{0}DataBase checked.", name);
                updateSettingsVersion();
                log.log(Level.INFO, "create {0} version {1}", new Object[]{name, dbversion});
            } else {
                // 既存DB引き継ぎの場合はdbversionだけ上げてv2->3の処理へ
                log.log(Level.INFO, "convert {0} version 1 -> 2 start", name);
                updateSettingsVersion();
                log.log(Level.INFO, "convert {0} version 1 -> 2 complete", name);
            }
        }
        // Version 2 -> 3
        if (dbversion == 2) {
            log.log(Level.INFO, "convert {0} version {1} -> {2} start", new Object[]{name, dbversion, dbversion + 1});
            // ユーザー状態テーブル追加
            MessageFormat mf = new MessageFormat("CREATE TABLE IF NOT EXISTS playerstats(most {0} NOT NULL, least {1} NOT NULL, logout {2} NOT NULL, PRIMARY KEY(most, least))");
            executeStatement(mf.format(new String[]{CTYPE.LONG.get(jdbc), CTYPE.LONG.get(jdbc), CTYPE.LONG.get(jdbc)}));
            // ユーザー統計データデーブル追加
            mf = new MessageFormat("CREATE TABLE IF NOT EXISTS statstable(most {0} NOT NULL, least {1} NOT NULL, name {2} NOT NULL, size {3} NOT NULL, data {4} NOT NULL, PRIMARY KEY(most, least))");
            executeStatement(mf.format(new String[]{CTYPE.LONG.get(jdbc), CTYPE.LONG.get(jdbc), CTYPE.STRING.get(jdbc), CTYPE.LONG.get(jdbc), CTYPE.BLOB.get(jdbc)}));
            // ユーザー実績データテーブル追加
            mf = new MessageFormat("CREATE TABLE IF NOT EXISTS advtable(most {0} NOT NULL, least {1} NOT NULL, name {2} NOT NULL, size {3} NOT NULL, data {4} NOT NULL, PRIMARY KEY(most, least))");
            executeStatement(mf.format(new String[]{CTYPE.LONG.get(jdbc), CTYPE.LONG.get(jdbc), CTYPE.STRING.get(jdbc), CTYPE.LONG.get(jdbc), CTYPE.BLOB.get(jdbc)}));
            // 既存テーブルからlogoutを分離(playerstatsへ)、
            renameTable("datatable", "datatable_");
            mf = new MessageFormat("CREATE TABLE IF NOT EXISTS datatable(most {0} NOT NULL, least {1} NOT NULL, name {2} NOT NULL, size {3} NOT NULL, data {4} NOT NULL, PRIMARY KEY(most, least))");
            executeStatement(mf.format(new String[]{CTYPE.LONG.get(jdbc), CTYPE.LONG.get(jdbc), CTYPE.STRING.get(jdbc), CTYPE.LONG.get(jdbc), CTYPE.BLOB.get(jdbc)}));

            PreparedStatement prep = con.prepareStatement("SELECT * FROM datatable_");
            PreparedStatement prep2 = con.prepareStatement("REPLACE INTO datatable VALUES (?, ?, ?, ?, ?)");
            PreparedStatement prep3 = con.prepareStatement("REPLACE INTO playerstats VALUES (?, ?, ?)");
            try {
                // 実行
                ResultSet rs = prep.executeQuery();
                try {
                    // 結果取得
                    while (rs.next()) {
                        prep2.setLong(1, rs.getLong("most"));
                        prep2.setLong(2, rs.getLong("least"));
                        prep2.setString(3, rs.getString("name"));
                        prep2.setLong(4, rs.getLong("size"));
                        prep2.setBlob(5, rs.getBlob("data"));
                        prep3.setLong(1, rs.getLong("most"));
                        prep3.setLong(2, rs.getLong("least"));
                        prep3.setLong(3, rs.getLong("logout"));
                        prep2.executeUpdate();
                        prep3.executeUpdate();
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
                prep2.close();
                prep3.close();
                throw ex;
                // ロールバックは上位のスーパークラスでやる
            }
            prep.close();
            dropTable("datatable_");

            updateSettingsVersion();
            log.log(Level.INFO, "convert {0} version {1} -> {2} complete", new Object[]{name, dbversion - 1, dbversion});
        }
    }

    /**
     * プレイヤー固有ファイル保存
     * @param uuid プレイヤーUUID
     * @param files 保存ファイル
     * @throws SQLException
     * @throws java.io.FileNotFoundException
     */
    public void savePlayerData(Connection con, UUID uuid, PlayerFileSet files) throws SQLException, FileNotFoundException, IOException {
        // SQLコンパイル
        PreparedStatement prep1 = con.prepareStatement("REPLACE INTO playerstats VALUES (?, ?, ?)");
        PreparedStatement prep2 = con.prepareStatement("REPLACE INTO datatable VALUES (?, ?, ?, ?, ?)");
        PreparedStatement prep3 = con.prepareStatement("REPLACE INTO statstable VALUES (?, ?, ?, ?, ?)");
        PreparedStatement prep4 = con.prepareStatement("REPLACE INTO advtable VALUES (?, ?, ?, ?, ?)");
        try {
            prep1.setLong(1, uuid.getMostSignificantBits());
            prep1.setLong(2, uuid.getLeastSignificantBits());
            prep1.setBoolean(3, true);

            prep2.setLong(1, uuid.getMostSignificantBits());
            prep2.setLong(2, uuid.getLeastSignificantBits());
            prep2.setString(3, files.profile.getName());
            prep2.setLong(4, files.profile.length());
            byte[] buf = new byte[(int)files.profile.length()];
            FileInputStream fis = new FileInputStream(files.profile);
            fis.read(buf, 0, (int) files.profile.length());
            fis.close();
            prep2.setBytes(5, buf);

            prep3.setLong(1, uuid.getMostSignificantBits());
            prep3.setLong(2, uuid.getLeastSignificantBits());
            prep3.setString(3, files.stats.getName());
            prep3.setLong(4, files.stats.length());
            buf = new byte[(int)files.stats.length()];
            fis = new FileInputStream(files.stats);
            fis.read(buf, 0, (int) files.stats.length());
            fis.close();
            prep3.setBytes(5, buf);

            prep4.setLong(1, uuid.getMostSignificantBits());
            prep4.setLong(2, uuid.getLeastSignificantBits());
            prep4.setString(3, files.adv.getName());
            prep4.setLong(4, files.adv.length());
            buf = new byte[(int)files.adv.length()];
            fis = new FileInputStream(files.adv);
            fis.read(buf, 0, (int) files.adv.length());
            fis.close();
            prep4.setBytes(5, buf);

            // 実行
            prep1.executeUpdate();
            prep2.executeUpdate();
            prep3.executeUpdate();
            prep4.executeUpdate();
        } catch (SQLException | FileNotFoundException ex) {
            // 抜けるため後処理
            prep1.close();
            prep2.close();
            prep3.close();
            prep4.close();
            // 投げなおして上位で異常検知させる
            throw ex;
        }
        // 後処理
        prep1.close();
        prep2.close();
        prep3.close();
        prep4.close();
    }

    /**
     * プレイヤー固有ファイル読み出し
     * @param uuid プレイヤーUUID
     * @param files ファイルパスを含むファイルインスタンス
     * @throws SQLException
     * @throws IOException
     */
    public void loadPlayerData(Connection con, UUID uuid, PlayerFileSet files) throws SQLException, IOException {
        // SQLコンパイル
        PreparedStatement prep1 = con.prepareStatement("SELECT * FROM datatable WHERE most = ? AND least = ?");
        PreparedStatement prep2 = con.prepareStatement("SELECT * FROM statstable WHERE most = ? AND least = ?");
        PreparedStatement prep3 = con.prepareStatement("SELECT * FROM advtable WHERE most = ? AND least = ?");
        try {
            prep1.setLong(1, uuid.getMostSignificantBits());
            prep1.setLong(2, uuid.getLeastSignificantBits());
            prep2.setLong(1, uuid.getMostSignificantBits());
            prep2.setLong(2, uuid.getLeastSignificantBits());
            prep3.setLong(1, uuid.getMostSignificantBits());
            prep3.setLong(2, uuid.getLeastSignificantBits());
            // 実行
            ResultSet rs = prep1.executeQuery();
            files.profile.delete();
            files.profile.createNewFile();
            FileOutputStream fos = new FileOutputStream(files.profile);
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
            if (fos != null) {
                fos.close();
            }
            rs.close();
            rs = prep2.executeQuery();
            files.stats.delete();
            files.stats.createNewFile();
            fos = new FileOutputStream(files.stats);
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
            rs = prep3.executeQuery();
            files.adv.delete();
            files.adv.createNewFile();
            fos = new FileOutputStream(files.adv);
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
            prep1.close();
            prep2.close();
            prep3.close();
            throw ex;
        }
        prep1.close();
        prep2.close();
        prep3.close();
    }

    /**
     * プレイヤー固有ファイル照会/profileデータだけで見る
     * @param uuid プレイヤーUUID
     * @return レコード有無
     * @throws SQLException
     */
    public boolean existPlayerData(Connection con, UUID uuid) throws SQLException {
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
     * プレイヤーログアウト状態更新(ログアウト開始)
     * @param uuid プレイヤーUUID
     * @throws SQLException
     */
    public void logoutPlayer(Connection con, UUID uuid) throws SQLException {
        // SQLコンパイル
        PreparedStatement prep = con.prepareStatement("UPDATE playerstats SET logout = ? WHERE most = ? AND least = ?");
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
     * プレイヤーログアウト状態照会
     * @param uuid プレイヤーUUID
     * @return ログアウト済みかどうか false:ログアウト処理中 true:ログアウト済み
     * @throws SQLException
     */
    public boolean isPlayerAfterLogout(Connection con, UUID uuid) throws SQLException {
        // SQLコンパイル
        boolean result = true;
        PreparedStatement prep = con.prepareStatement("SELECT logout FROM playerstats WHERE most = ? AND least = ?");
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

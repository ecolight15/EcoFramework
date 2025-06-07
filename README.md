# EcoFramework

Minecraftプラグイン開発のための共通フレームワーク

## 概要

EcoFrameworkは、Minecraft Spigot/Bukkitプラグインの開発を効率化するための共通フレームワークです。プラグイン開発でよく使用される機能（コマンド処理、設定管理、データベース操作、イベント処理など）を統一されたAPIで提供し、開発者がビジネスロジックに集中できるようにします。

## 主な機能

### フレームワークコンポーネント

- **PluginFrame** - プラグインのベースクラス。JavaPluginを拡張し、フレームワークの機能を統合
- **CommandFrame** - コマンド処理とタブ補完の統一フレームワーク
- **ConfigFrame** - 設定ファイル管理とリロード機能
- **DatabaseFrame** - SQLite/MySQL対応のデータベース抽象化レイヤー
- **ListenerFrame** - イベントリスナーの管理フレームワーク
- **TimerFrame** - スケジューリングとタイマー処理
- **LoggerFrame** - ファイルロギング機能
- **ManagerFrame** - 各コンポーネントの統合管理

### データベースサポート

- **SQLite** - デフォルトのファイルベースデータベース
- **MySQL** - HikariCPによる高性能コネクションプール
- **PlayerFileStore** - プレイヤー固有データの自動管理

### 非同期処理

- **AsyncFrame** - 非同期処理の基盤
- **PayloadFrame** - メインスレッドと非同期スレッド間のデータ交換
- **MessageAsyncFrame** - 非同期メッセージング

### その他の機能

- **BungeeController** - BungeeCordサーバー間通信サポート
- プラグイン間の依存関係管理
- 設定ファイルの自動リロード
- プラグインの動的再起動サポート

## インストール

### 前提条件

- Java 8以上
- Spigot/Bukkit 1.18.2以上

### サーバーへの導入

1. [Releases](https://github.com/ecolight15/EcoFramework/releases)から最新のEcoFramework.jarをダウンロード
2. サーバーの`plugins`フォルダにjarファイルを配置
3. サーバーを再起動

### 他のプラグインからの利用

EcoFrameworkを使用するプラグインのplugin.ymlに以下を追加：

```yaml
depend: [EcoFramework]
```

## 設定

### config.yml

```yaml
userdatadb:
  # プレイヤーデータのデータベース保存を有効化
  use: false
  
  # データベースタイプ (sqlite または mysql)
  db: "sqlite"
  
  # データベース名 (sqlite: ファイル名, mysql: データベース名)
  name: "userdata.db"
  
  # MySQLサーバー接続設定 (MySQL使用時のみ)
  server: "localhost:3306"
  user: "username"
  pass: "password"
```

### データベース設定例

#### SQLite（推奨）
```yaml
userdatadb:
  use: true
  db: "sqlite"
  name: "playerdata.db"
```

#### MySQL
```yaml
userdatadb:
  use: true
  db: "mysql"
  name: "minecraft_data"
  server: "localhost:3306"
  user: "minecraft_user"
  pass: "secure_password"
```

## コマンド

| コマンド | 説明 | 権限 |
|---------|------|------|
| `/ecoframework` | フレームワークメンテナンスコマンド | `ecoframework` |
| `/ecoframework permissions` | 全EcoFrameworkプラグインの権限を表示 | `ecoframework.permissions` |
| `/ecoframework accept` | 確認コマンドを承認 | `ecoframework.accept` |
| `/ecoframework cancel` | 確認コマンドをキャンセル | `ecoframework.cancel` |

## 開発者向けAPI

### Maven依存関係の追加

EcoFrameworkはMavenプロジェクトの依存関係として指定することができます。

#### リポジトリの指定
```xml
<repository>
    <id>eco-plugin</id>
    <url>http://ecolight15.github.io/mvn_rep/</url>
</repository>
```

#### 依存関係の指定  
```xml
<dependency>
    <groupId>jp.minecraftuser</groupId>
    <artifactId>EcoFramework</artifactId>
    <version>0.30</version>
    <scope>provided</scope>
</dependency>
```

### 基本的な使用方法

EcoFrameworkを使用したプラグインの作成：

```java
public class MyPlugin extends PluginFrame {
    
    @Override
    public void onEnable() {
        initialize();
        getLogger().info("MyPlugin が有効化されました");
    }
    
    @Override
    public void onDisable() {
        disable();
        getLogger().info("MyPlugin が無効化されました");
    }
    
    @Override
    protected void initializeConfig() {
        // 設定ファイルの初期化
        ConfigFrame config = new MyPluginConfig(this);
        config.registerString("example.setting");
        registerPluginConfig(config);
    }
    
    @Override
    protected void initializeCommand() {
        // コマンドの初期化
        CommandFrame cmd = new MyCommand(this, "mycommand");
        registerPluginCommand(cmd);
    }
    
    @Override
    protected void initializeListener() {
        // イベントリスナーの初期化
        registerPluginListener(new MyListener(this));
    }
}
```

### コマンドの実装

```java
public class MyCommand extends CommandFrame {
    
    public MyCommand(PluginFrame plg, String name) {
        super(plg, name);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (!checkRange(sender, args, 0, 1)) return true;
        
        // コマンド処理をここに実装
        sendPluginMessage(sender, "コマンドが実行されました: " + args[0]);
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("option1", "option2", "option3");
        }
        return new ArrayList<>();
    }
}
```

### データベースの使用

```java
public class MyDatabase extends DatabaseFrame {
    
    public MyDatabase(PluginFrame plg, String name) throws ClassNotFoundException, SQLException {
        super(plg, "mydatabase.db", name);
    }
    
    @Override
    protected void migrationData(Connection con) throws SQLException {
        // dbversion: フレームワーク管理のDB版数（ユーザーが設定可能）
        // justCreated: 新規作成された直後かどうかを示すbool値
        
        // 例: 版数管理によるDBマイグレーション処理
        if (justCreated) {
            // 新規作成時は最新のテーブル構造を作成
            executeStatement("CREATE TABLE user_data (" +
                            "id INTEGER PRIMARY KEY," +
                            "player_name TEXT NOT NULL," +
                            "email TEXT," +
                            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "data TEXT)");
            
            // 新規作成時は最新版に設定
            updateSettingsVersion(con, 2);
            
        } else {
            // 既存DBの場合、版数に応じてマイグレーション
            if (dbversion == 1) {
                // 版数1から2へのマイグレーション
                executeStatement("ALTER TABLE user_data ADD COLUMN email TEXT");
                executeStatement("ALTER TABLE user_data ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
                
                // 版数をインクリメント
                updateSettingsVersion(con);
            }
            
            // 版数2以降の場合は何もしない（最新版）
        }
        
        // この関数を抜ける段階で、新規作成・既存更新に関わらず
        // 同じテーブル構造になることを保証
    }
    
    public void saveUserData(String playerName, String data) {
        try (Connection con = getConnection()) {
            PreparedStatement stmt = con.prepareStatement(
                "INSERT OR REPLACE INTO user_data (player_name, data) VALUES (?, ?)");
            stmt.setString(1, playerName);
            stmt.setString(2, data);
            stmt.executeUpdate();
        } catch (SQLException e) {
            getLogger().warning("データ保存エラー: " + e.getMessage());
        }
    }
}
```

### イベントリスナーの実装

```java
public class MyListener extends ListenerFrame {
    
    public MyListener(PluginFrame plg) {
        super(plg);
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        sendPluginMessage(player, "EcoFrameworkプラグインへようこそ！");
    }
}
```

## ビルド方法

### 前提条件

- JDK 8以上
- Apache Maven 3.6以上

### ビルド手順

```bash
# リポジトリをクローン
git clone https://github.com/ecolight15/EcoFramework.git
cd EcoFramework

# 依存関係の解決とコンパイル
mvn clean compile

# JARファイルの作成
mvn package

# ビルドされたJARファイルは target/EcoFramework-*.jar に出力されます
```

### 依存関係

- Spigot API 1.18.2
- MySQL Connector/J 8.0.28
- HikariCP 3.2.0

## プロジェクト構造

```
src/main/java/jp/minecraftuser/ecoframework/
├── PluginFrame.java           # メインフレームワーククラス
├── CommandFrame.java          # コマンドフレームワーク
├── ConfigFrame.java           # 設定管理フレームワーク
├── DatabaseFrame.java         # データベース抽象化レイヤー
├── ListenerFrame.java         # イベントリスナーフレームワーク
├── LoggerFrame.java           # ロギングフレームワーク
├── TimerFrame.java            # タイマーフレームワーク
├── ManagerFrame.java          # コンポーネント管理
├── Utl.java                   # ユーティリティクラス
├── async/                     # 非同期処理関連
│   ├── AsyncFrame.java
│   ├── PayloadFrame.java
│   └── MessageAsyncFrame.java
├── db/                        # データベース関連
│   ├── JdbcBase.java
│   ├── JdbcSqlite.java
│   └── JdbcMySQL.java
├── store/                     # プレイヤーデータストレージ
│   ├── PlayerFileStore.java
│   └── PlayerDataFileStoreListener.java
├── bungee/                    # BungeeCord連携
│   └── BungeeController.java
├── exception/                 # 例外クラス
├── iface/                     # インターフェース
└── plugin/                    # プラグイン本体
    ├── EcoFramework.java
    └── EcoFrameworkConfig.java
```

## サンプルプロジェクト

EcoFrameworkを使用したサンプルプラグインは[こちら](https://github.com/ecolight15)の他のリポジトリで確認できます。

## ライセンス

このプロジェクトは[GNU Lesser General Public License v3.0](LICENSE)の下でライセンスされています。

## コントリビューション

1. このリポジトリをフォーク
2. 新しいブランチを作成 (`git checkout -b feature/new-feature`)
3. 変更をコミット (`git commit -am 'Add new feature'`)
4. ブランチにプッシュ (`git push origin feature/new-feature`)
5. プルリクエストを作成

## サポート

- バグ報告や機能要求は[Issues](https://github.com/ecolight15/EcoFramework/issues)にて受け付けています
- 使用方法についての質問も歓迎します

## 作者

- **ecolight** - *初期開発* - [ecolight15](https://github.com/ecolight15)

## 履歴

詳細な変更履歴は[Releases](https://github.com/ecolight15/EcoFramework/releases)をご確認ください。
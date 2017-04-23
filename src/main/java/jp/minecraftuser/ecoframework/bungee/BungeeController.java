
package jp.minecraftuser.ecoframework.bungee;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import jp.minecraftuser.ecoframework.PluginFrame;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 * BungeeCord制御フレーム
 * 基本的にプレイヤーのコネクションを通してBungeeCordと通信する機能であるため
 * 全てのメッセージチャンネル操作にOnlineプレイヤーインスタンスが必要になる
 * このためプレイヤーが居なくなると制御権が無くなるため注意
 * @author ecolight
 */
public abstract class BungeeController implements PluginMessageListener {
    private final PluginFrame plg;
    private final Logger log;
    private boolean regist = false;

    /**
     * コンストラクタ
     * @param plg_ プラグインフレームインスタンス
     */
    public BungeeController(PluginFrame plg_) {
        plg = plg_;
        log = plg.getLogger();
    }
    
    /**
     * BungeeCordのメッセージ受信登録をする
     */
    public void registerBungeeListener() {
        if (!regist) {
            plg.getServer().getMessenger().registerIncomingPluginChannel(plg, "BungeeCord", this);
            plg.getServer().getMessenger().registerOutgoingPluginChannel(plg, "BungeeCord");
            regist = true;
            
            // 
        }
    }
    
    /**
     * サーバー接続指示
     * @param p プレイヤー
     * @param server サーバー名
     */
    public void connectServer(Player p, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);
        p.sendPluginMessage(plg, "BungeeCord", out.toByteArray());
    }

    /**
     * 他プレイヤーサーバー接続指示
     * @param p プレイヤー
     * @param target プレイヤー指定
     * @param server サーバー名
     */
    public void connectOtherPlayerServer(Player p, String target, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ConnectOther");
        out.writeUTF(target);
        out.writeUTF(server);
        p.sendPluginMessage(plg, "BungeeCord", out.toByteArray());
    }

    /**
     * プレイヤーIP取得指示
     * @param p プレイヤー
     */
    public void getPlayerIP(Player p) {
        if (regist) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("IP");
            p.sendPluginMessage(plg, "BungeeCord", out.toByteArray());
        } else {
            log.log(Level.WARNING, "Require register this listener.{0}", this.getClass().getName());
        }
    }

    /**
     * プレイヤー数取得指示
     * @param p プレイヤー
     * @param server サーバー名
     */
    public void getPlayerCount(Player p, String server) {
        if (regist) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("PlayerCount");
            out.writeUTF(server);
            p.sendPluginMessage(plg, "BungeeCord", out.toByteArray());
        } else {
            log.log(Level.WARNING, "Require register this listener.{0}", this.getClass().getName());
        }
    }

    /**
     * プレイヤーリスト取得指示
     * @param p プレイヤー
     * @param server サーバー名
     */
    public void getPlayerList(Player p, String server) {
        if (regist) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("PlayerList");
            out.writeUTF(server);
            p.sendPluginMessage(plg, "BungeeCord", out.toByteArray());
        } else {
            log.log(Level.WARNING, "Require register this listener.{0}", this.getClass().getName());
        }
    }

    /**
     * サーバーリスト取得指示
     * @param p プレイヤー
     */
    public void getServers(Player p) {
        if (regist) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("GetServers");
            p.sendPluginMessage(plg, "BungeeCord", out.toByteArray());
        } else {
            log.log(Level.WARNING, "Require register this listener.{0}", this.getClass().getName());
        }
    }

    /**
     * メッセージ送信指示
     * 直接指定プレイヤーのクライアントに送信されるため
     * プレイヤーがいるサーバーのプラグインはフックできなさそう
     * @param p プレイヤー
     * @param target 送信先指定
     * @param message メッセージ送信
     */
    public void sendMessage(Player p, String target, String message) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Message");
        out.writeUTF(target);
        out.writeUTF(message);
        p.sendPluginMessage(plg, "BungeeCord", out.toByteArray());
    }

    /**
     * サーバー取得指示
     * @param p プレイヤー
     */
    public void getServer(Player p) {
        if (regist) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("GetServer");
            p.sendPluginMessage(plg, "BungeeCord", out.toByteArray());
        } else {
            log.log(Level.WARNING, "Require register this listener.{0}", this.getClass().getName());
        }
    }

    /**
     * フォワード指示(全サーバーブロードキャスト)
     * 呼び出し元で次の要領で任意のデータを設定する。
     * DataOutputStream msgout = new DataOutputStream(msgbytes);
     * msgout.writeUTF("Some kind of data here");
     * msgout.writeShort(123);
     * @param p プレイヤー
     * @param channel チャンネル(独自の)
     * @param msgbytes (送信データ)
     */
    public void forward(Player p, String channel, ByteArrayOutputStream msgbytes) {
        if (regist) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Forward");
            out.writeUTF("ALL");
            out.writeUTF(channel);
            out.writeShort(msgbytes.toByteArray().length);
            out.write(msgbytes.toByteArray());
            p.sendPluginMessage(plg, "BungeeCord", out.toByteArray());
            log.info("BungeeCord forward broadcasting");
        } else {
            log.log(Level.WARNING, "Require register this listener.{0}", this.getClass().getName());
        }
    }

    /**
     * フォワード指示(指定サーバー)
     * 呼び出し元で次の要領で任意のデータを設定する。
     * DataOutputStream msgout = new DataOutputStream(msgbytes);
     * msgout.writeUTF("Some kind of data here");
     * msgout.writeShort(123);
     * @param p プレイヤー
     * @param server サーバー
     * @param channel チャンネル(独自の)
     * @param msgbytes (送信データ)
     */
    public void forward(Player p, String server, String channel, ByteArrayOutputStream msgbytes) {
        if (regist) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Forward");
            out.writeUTF(server);
            out.writeUTF(channel);
            out.writeShort(msgbytes.toByteArray().length);
            out.write(msgbytes.toByteArray());
            p.sendPluginMessage(plg, "BungeeCord", out.toByteArray());
            log.info("BungeeCord server forwarding");
        } else {
            log.log(Level.WARNING, "Require register this listener.{0}", this.getClass().getName());
        }
    }

    /**
     * フォワード指示(指定サーバー)
     * 呼び出し元で次の要領で任意のデータを設定する。
     * DataOutputStream msgout = new DataOutputStream(msgbytes);
     * msgout.writeUTF("Some kind of data here");
     * msgout.writeShort(123);
     * @param p プレイヤー
     * @param target 送信先プレイヤー名
     * @param channel チャンネル(独自の)
     * @param msgbytes (送信データ)
     */
    public void forwardPlayer(Player p, String target, String channel, ByteArrayOutputStream msgbytes) {
        if (regist) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("ForwardToPlayer");
            out.writeUTF(target);
            out.writeUTF(channel);
            out.writeShort(msgbytes.toByteArray().length);
            out.write(msgbytes.toByteArray());
            p.sendPluginMessage(plg, "BungeeCord", out.toByteArray());
            log.info("BungeeCord player forwarding");
        } else {
            log.log(Level.WARNING, "Require register this listener.{0}", this.getClass().getName());
        }
    }

    /**
     * UUID取得指示
     * @param p プレイヤー
     */
    public void getUUID(Player p) {
        if (regist) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("UUID");
            p.sendPluginMessage(plg, "BungeeCord", out.toByteArray());
        } else {
            log.log(Level.WARNING, "Require register this listener.{0}", this.getClass().getName());
        }
    }

    /**
     * 他プレイヤーUUID取得指示
     * @param p プレイヤー
     * @param target 指定名
     */
    public void getOtherPlayerUUID(Player p, String target) {
        if (regist) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("UUIDOther");
            out.writeUTF(target);
            p.sendPluginMessage(plg, "BungeeCord", out.toByteArray());
        } else {
            log.log(Level.WARNING, "Require register this listener.{0}", this.getClass().getName());
        }
    }

    /**
     * サーバーIP取得指示
     * @param p プレイヤー
     * @param server サーバー名
     */
    public void getServerIP(Player p, String server) {
        if (regist) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("ServerIP");
            out.writeUTF(server);
            p.sendPluginMessage(plg, "BungeeCord", out.toByteArray());
        } else {
            log.log(Level.WARNING, "Require register this listener.{0}", this.getClass().getName());
        }
    }

    /**
     * Kick指示
     * @param p プレイヤー
     * @param target Kick対象
     * @param reason 理由
     */
    public void kickPlayer(Player p, String target, String reason) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("KickPlayer");
        out.writeUTF(target);
        out.writeUTF(reason);
        p.sendPluginMessage(plg, "BungeeCord", out.toByteArray());
    }

    /**
     * BungeeCordメッセージ受信処理
     * @param channel チャンネル名(BungeeCord以外は弾く)
     * @param player 受信プレイヤー
     * @param message 受信メッセージ
     */
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) {
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subchannel = in.readUTF();
        switch (subchannel) {
            case "IP":
                String ip = in.readUTF();
                int port = in.readInt();
                receiveIP(player, ip, port);
                break;
            case "PlayerCount":
                String server = in.readUTF();
                int playercount = in.readInt();
                receivePlayerCount(player, server, playercount);
                break;
            case "PlayerList":
                String server2 = in.readUTF();
                String[] playerList = in.readUTF().split(", ");
                receivePlayerList(player, server2, playerList);
                break;
            case "GetServers":
                String[] serverList = in.readUTF().split(", ");
                receiveGetServers(player, serverList);
                break;
            case "GetServer":
                String servername = in.readUTF();
                receiveGetServer(player, servername);
                break;
            case "ForwardToPlayer":
                String subChannel2 = in.readUTF();
                short len2 = in.readShort();
                byte[] msgbytes2 = new byte[len2];
                in.readFully(msgbytes2);
                DataInputStream msgin2 = new DataInputStream(new ByteArrayInputStream(msgbytes2));
                receiveForward(player, subChannel2, msgin2);
                break;
            case "UUID":
                String uuid = in.readUTF();
                receiveUUID(player, uuid);
                break;
            case "UUIDOther":
                String playerName = in.readUTF();
                String uuid2 = in.readUTF();
                receiveUUIDOther(player, playerName, uuid2);
                break;
            case "ServerIP":
                String serverName = in.readUTF();
                String ip2 = in.readUTF();
                int port2 = in.readUnsignedShort();
                receiveServerIP(player, serverName, ip2, port2);
                break;
            // 他のサブチャンネルはforwardによる転送とみなす
            default:
                short len = in.readShort();
                byte[] msgbytes = new byte[len];
                in.readFully(msgbytes);
                DataInputStream msgin = new DataInputStream(new ByteArrayInputStream(msgbytes));
                receiveForward(player, subchannel, msgin);
                break;
        }
    }
    
    /**
     * IP受信処理
     * @param p プレイヤー
     * @param ip IPアドレス
     * @param port ポート番号
     */
    protected void receiveIP(Player p, String ip, int port) {
        // optional method
    }
    
    /**
     * PlayerCount受信処理
     * @param p プレイヤー
     * @param server サーバー名
     * @param playercount プレイヤー数
     */
    protected void receivePlayerCount(Player p, String server, int playercount) {
        // optional method
    }
    
    /**
     * PlayerList受信処理
     * @param p プレイヤー
     * @param server サーバー名
     * @param playerList プレイヤー数
     */
    protected void receivePlayerList(Player p, String server, String[] playerList) {
        // optional method
    }
    
    /**
     * GetServers受信処理
     * @param p プレイヤー
     * @param serverList サーバーリスト
     */
    protected void receiveGetServers(Player p, String[] serverList) {
        // optional method
    }
    
    /**
     * GetServer受信処理
     * @param p プレイヤー
     * @param servername サーバー
     */
    protected void receiveGetServer(Player p, String servername) {
        // optional method
    }
    
    /**
     * Forward受信処理
     * 以下の要領で書き込みデータと同じ順序で取り出す
     * String somedata = msgin.readUTF();
     * short somenumber = msgin.readShort();
     * @param p プレイヤー
     * @param subChannel チャンネル(独自の)
     * @param msgin 受信データ
     */
    protected void receiveForward(Player p, String subChannel, DataInputStream msgin) {
        // optional method
    }
    
    /**
     * UUID受信処理
     * @param p プレイヤー
     * @param uuid UUID
     */
    protected void receiveUUID(Player p, String uuid) {
        // optional method
    }
    
    /**
     * 他プレイヤーUUID受信処理
     * @param p プレイヤー
     * @param playerName 取得プレイヤー名
     * @param uuid UUID
     */
    protected void receiveUUIDOther(Player p, String playerName, String uuid) {
        // optional method
    }
    
    /**
     * ServerIP受信処理
     * @param p プレイヤー
     * @param serverName サーバー名
     * @param ip IPアドレス
     * @param port ポート番号
     */
    protected void receiveServerIP(Player p, String serverName, String ip, int port) {
        // optional method
    }
}

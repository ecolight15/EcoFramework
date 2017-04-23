
package jp.minecraftuser.ecoframework.async;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import jp.minecraftuser.ecoframework.PluginFrame;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * 非同期メッセージ通知フレーム
 * @author ecolight
 */
public abstract class MessageAsyncFrame extends AsyncFrame {
    private ConcurrentLinkedQueue<MessagePayload> queue = new ConcurrentLinkedQueue<>();
    private Long count;
    private Long sleep;
    private Server server;
    /**
     * コンストラクタ
     * @param plg_
     */
    public MessageAsyncFrame(PluginFrame plg_, String name_) {
        super(plg_, name_);

        // 呼び出し回数を削減するためサーバーインスタンスを確保しておく
        server = plg.getServer();
        Player player;
        // message-sending-max設定値があれば使用する
        conf.registerLong("framework-message-sending-max", true);
        count = conf.getLong("framework-message-sending-max");
        // 無ければ1回あたり50回送信のみ対応
        if (count == null) count = 50L;
        log.info("framework-message-sending-max:"+count);

        // message-receive-interval設定値があれば使用する
        conf.registerLong("framework-message-receive-interval", true);
        sleep = conf.getLong("framework-message-receive-interval");
        // 無ければ1回あたり1ミリ秒Sleepする
        if (sleep == null) sleep = 1L;
        log.info("framework-message-receive-interval:"+sleep);
    }

    /**
     * 親スレッド(メインスレッド)処理
     * Bukkitスケジューラにより定期呼び出し
     */
    @Override
    protected void parentRun() {
        // 指定回数以内でキューが空になるまで処理する
        long cnt = count;
        while (!queue.isEmpty()) {
            MessagePayload msg = queue.poll();
            Player p = server.getPlayer(msg.getUUID());
            if ((p != null) && (p.isOnline())) {
                p.sendMessage(msg.getMessage());
            }
            cnt--;
            if (cnt <= 0) {
                break;
            }
        }
    }

    /**
     * 子スレッド処理
     * 生成時に単一呼び出し
     */
    @Override
    protected void childRun() {
        // 親スレッド側は子スレッドが停止しても継続動作させる
        setPersist();
        
        // 停止指示があるまで処理する
        while (true) {
            synchronized (this) {
                if (childend) break;
            }

            // キューに値が存在する場合は延々と処理する(CPU負荷にならない程度のSleepを挟む)
            MessagePayload msg = queue.poll();
            if (msg != null) {
                    executeProcess(msg);
            }
            // スレッドのSleep(ミリ秒)
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException ex) {
                Logger.getLogger(MessageAsyncFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * キューへのメッセージ追加処理
     * 親スレッドインスタンスへの追加はプレイヤーへのsendMessageを意味する
     * 子スレッドインスタンスへの追加は子スレッド側でのメッセージ加工処理への依頼を意味する
     * @param msg メッセージペイロードインスタンス
     */
    private void addMessage(MessagePayload msg) {
        queue.add(msg);
    }

    /**
     * 親スレッドのキューへのメッセージ追加処理
     * @param msg メッセージペイロードインスタンス
     */
    public void receiveMessage(MessagePayload msg) {
        ((MessageAsyncFrame)parentFrame).addMessage(msg);
    }

    /**
     * 子スレッドのキューへのメッセージ追加処理
     * @param msg メッセージペイロードインスタンス
     */
    public void sendMessage(MessagePayload msg) {
        ((MessageAsyncFrame)childFrame).addMessage(msg);
    }

    /**
     * メッセージ加工処理
     * @param msg メッセージペイロードインスタンス
     */
    protected abstract void executeProcess(MessagePayload msg);
}

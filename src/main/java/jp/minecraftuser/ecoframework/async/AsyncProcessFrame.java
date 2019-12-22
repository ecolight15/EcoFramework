
package jp.minecraftuser.ecoframework.async;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import jp.minecraftuser.ecoframework.PluginFrame;
import org.bukkit.Server;
import org.bukkit.entity.Player;

/**
 * 非同期Data加工フレーム
 * @author ecolight
 */
public abstract class AsyncProcessFrame extends AsyncFrame {
    private ConcurrentLinkedQueue<PayloadFrame> queue = new ConcurrentLinkedQueue<>();
    private Long count;
    private Long sleep;
    private Server server;
    /**
     * 親スレッド用コンストラクタ
     * @param plg_ プラグインフレームインスタンス
     * @param name_ 名前
     */
    public AsyncProcessFrame(PluginFrame plg_, String name_) {
        super(plg_, name_);

        // 呼び出し回数を削減するためサーバーインスタンスを確保しておく
        server = plg.getServer();
        Player player;
        // framework-sending-max設定値があれば使用する
        conf.registerLong("framework-sending-max", true);
        count = conf.getLong("framework-sending-max");
        // 無ければ1回あたり50回送信のみ対応
        if (count == null) count = 50L;
        log.info("framework-sending-max:"+count);

        // framework-receive-interval設定値があれば使用する
        conf.registerLong("framework-receive-interval", true);
        sleep = conf.getLong("framework-receive-interval");
        // 無ければ1回あたり1ミリ秒Sleepする
        if (sleep == null) sleep = 1L;
        log.info("framework-receive-interval:"+sleep);
    }

    /**
     * 子スレッド用コンストラクタ
     * @param plg_ プラグインフレームインスタンス
     * @param name_ 名前
     * @param frame_ 親スレッド用非同期処理フレームインスタンス
     */
    public AsyncProcessFrame(PluginFrame plg_, String name_, AsyncFrame frame_) {
        super(plg_, name_, frame_);
        // 呼び出し回数を削減するためサーバーインスタンスを確保しておく
        server = plg.getServer();
        Player player;
        // framework-sending-max設定値があれば使用する
        conf.registerLong("framework-sending-max", true);
        count = conf.getLong("framework-sending-max");
        // 無ければ1回あたり50回送信のみ対応
        if (count == null) count = 50L;
        log.info("framework-sending-max:"+count);

        // framework-receive-interval設定値があれば使用する
        conf.registerLong("framework-receive-interval", true);
        sleep = conf.getLong("framework-receive-interval");
        // 無ければ1回あたり1ミリ秒Sleepする
        if (sleep == null) sleep = 1L;
        log.info("framework-receive-interval:"+sleep);
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
            PayloadFrame data = queue.poll();
            executeReceive(data);
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
            PayloadFrame data = queue.poll();
            if (data != null) {
                    executeProcess(data);
            }
            // スレッドのSleep(ミリ秒)
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException ex) {
                Logger.getLogger(AsyncProcessFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        finalizeProcess();
    }

    /**
     * キューへのData追加処理
     * 親スレッドインスタンスへの追加はプレイヤーへのsendDataを意味する
     * 子スレッドインスタンスへの追加は子スレッド側でのData加工処理への依頼を意味する
     * @param msg メッセージペイロードインスタンス
     */
    private void addData(PayloadFrame data_) {
        queue.add(data_);
    }

    /**
     * 親スレッドのキューへのData追加処理
     * @param data_ ペイロードインスタンス
     */
    public void receiveData(PayloadFrame data_) {
        ((AsyncProcessFrame)parentFrame).addData(data_);
    }

    /**
     * 子スレッドのキューへのData追加処理
     * @param data_ ペイロードインスタンス
     */
    public void sendData(PayloadFrame data_) {
        ((AsyncProcessFrame)childFrame).addData(data_);
    }

    /**
     * Data加工子スレッド側処理
     * @param data_ ペイロードインスタンス
     */
    protected abstract void executeProcess(PayloadFrame data_);

    /**
     * 子スレッド終了時処理
     */
    protected void finalizeProcess(){};

    /**
     * Data加工後親スレッド側処理
     * @param data_ ペイロードインスタンス
     */
    protected abstract void executeReceive(PayloadFrame data_);
}

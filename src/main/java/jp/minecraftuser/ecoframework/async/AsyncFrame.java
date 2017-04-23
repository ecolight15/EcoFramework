
package jp.minecraftuser.ecoframework.async;

import jp.minecraftuser.ecoframework.PluginFrame;
import jp.minecraftuser.ecoframework.TimerFrame;

/**
 * 汎用非同期処理クラス
 * @author ecolight
 */
public abstract class AsyncFrame extends TimerFrame {
    private boolean end = false;
    private boolean persist = false;
    protected boolean parent = true;
    protected AsyncFrame childFrame = null;
    protected AsyncFrame parentFrame = null;
    protected boolean childend = false;

    /**
     * コンストラクタ
     * @param plg_ プラグインインスタンス
     * @param name_ 名前
     */
    public AsyncFrame(PluginFrame plg_, String name_) {
        super(plg_, name_);
        // インスタンス生成時は子スレッドを生成する
        if ((childFrame == null) && (parent)) {
            childFrame = clone();
            // 子プロセスをキックする
            childFrame.runTaskAsynchronously(plg);
        }
    }

    public AsyncFrame(PluginFrame plg_, String name_, AsyncFrame frame_) {
        super(plg_, name_);
        parent = false;
        parentFrame = frame_;
    }

    /**
     * 停止指示処理
     * 子スレッドから親スレッドに対してコールされる
     */
    private final void stop() {
        synchronized (this) {
            if (persist) {
                log.info("detect child thread frame stopped. persist mode is valid so stop parent thread frame manually.");
            } else {
                end = true;
                log.info("set parent thread frame end flag.");
            }
        }
    }

    /**
     * 子スレッドへの停止指示
     * 子スレッドでは childend 変数を監視して ture になった場合は迅速に処理を完了すること
     */
    public final void childStop() {
        synchronized (this) {
            if (parent) {
                childFrame.childStop();
            } else {
                childend = true;
                log.info("set child thread frame end flag.");
            }
        }
    }

    /**
     * 親スレッド側処理の永続化指示
     * 子スレッド側処理完了後も親スレッド側の定期処理を継続したい場合に、子スレッド側処理が終わる前にどこかで呼んでおくこと。
     * また、その場合は停止の際にはメインスレッド側から親スレッド側タイマーインスタンスに別途cancel呼び出しすること。
     */
    public final void setPersist() {
        if (parent) {
            synchronized (this) {
                persist = true;
            }
        } else {
            parentFrame.setPersist();
        }
    }

    /**
     * spigot同期処理
     * spigotのスケジューラから同期呼び出しされるメソッド
     * タイマーの起動はrunTaskTimerで起動すること
     */
    @Override
    public final void run() {

        // 初回起動時は子スレッドを生成する
//        if ((childFrame == null) && (parent)) {
//            childFrame = clone();
//            childFrame.setChild(this);
//            // 子プロセスをキックする
//            childFrame.runTaskAsynchronously(plg);
//        }
        
        // 親スレッドでは継承先で実装する親スレッド用メソッドをコールする
        // runTaskTimerにより定期呼び出し
        if (parent) {
            parentRun();

            // 子スレッドから停止指示を受けている場合はキャンセル処理を行う
            synchronized (this) {
                if (end) {
                    cancel();
                }
            }
        }
        // 子スレッドでは継承先で実装する子スレッド用メソッドをコールする
        // runTaskAsynchronouslyで単発呼び出し
        else {
            log.info("Start child thread method.");
            childRun();
            log.info("Ended child thread method.");
            parentFrame.stop();
        }
    }

    /**
     * キャンセル処理
     * キャンセルの際に管理している子スレッドのタイマーもキャンセルする
     * @throws IllegalStateException
     */
    @Override
    public final synchronized void cancel() throws IllegalStateException {
        super.cancel();
        if (parent) {
            log.info("canceled parent thread.");
        } else {
            log.info("canceled child thread.");
        }
        if((childFrame != null) && (parent)) {
            childStop();
            childFrame.cancel();
        }
    }
    
    /**
     * 継承先のクラスのインスタンス(子スレッド用)を生成して返却する事
     * 親子間で共有リソースがある場合、マルチスレッドセーフな作りにすること
     * synchronizedにする、スレッドセーフ対応クラスを使用するなど
     * @return AsyncFrame継承クラスのインスタンス
     */
    @Override
    protected abstract AsyncFrame clone();

    /**
     * 親スレッド
     * 定期呼び出し処理
     */
    protected abstract void parentRun();
    
    /**
     * 子スレッド
     * 単発呼び出し処理
     */
    protected abstract void childRun();
    
}

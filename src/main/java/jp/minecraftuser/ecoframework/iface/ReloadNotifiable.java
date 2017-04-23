
package jp.minecraftuser.ecoframework.iface;

/**
 * ConfigFrameのReload時にFrameに対しての通知受付可能な事を示す
 * @author ecolight
 */
public interface ReloadNotifiable {

    /**
     * ConfigFrameまたはPluginFrameに登録した場合にReload通知を受け取るメソッド
     * Reload通知を受け取る場合、各フレームのコンストラクタ等から該当ConfigRrameのregisterNotifiableで登録しておくこと
     * ConfigFrameへの登録の場合、reloadNotifyメソッドの先頭でFrameの
     * super.reload()コールする事になると思われるが、その後にその他の処理を
     * 実装すると考えられるためsuper.reload()内の配下のReloadNotifiableへの
     * 通知が早すぎる懸念がある。
     * このため最初はboolean baseをtrueで受け取り、
     * trueの場合はsuper.reload()のコールのみ実施し、
     * falseの場合に本来の通知処理を実装する事で
     * super.reload()の実行→super.reload()内から自らのreloadNotifyをfalseで
     * 実行→配下の管理ReloadNotifiableインスタンスのreloadNotifyをtrueで実行
     * と順序性を整理する事にする。
     * **** 苦肉の策なので何とかしたい ****
     * @param base 基底呼び出し(ConfigFrameのみ)
     */
    default public void reloadNotify(boolean base) {
        // OptionalMethod
        
        // ConfigFrame実装の例
//        if (base) {
//            // baseがtrueなので最初に実行される
//            super.reload();
//        } else {
//            // super.reload()内から自身のコンフィグロード後に呼び出される
//            xxmap = new HashMap<>();
//            yymap = new HashMap<>();
//
//            try
//            {
//                loadConfiguration();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            // ここを抜けた後にsuper.reload()内で自分が管理するReloadNotifiable
//            // 登録クラスのreloadNotifyを全てコールする。
//        }
    }
    /**
     * ConfigFrame以外は本メソッドがコールされる
     */
    default public void reloadNotify() {
        // optional method
    }
}

package cn.vove7.jarvis.view.floatwindows

import android.graphics.drawable.AnimationDrawable
import android.os.Handler
import android.view.View
import cn.vove7.common.app.GlobalApp
import cn.vove7.common.appbus.AppBus
import cn.vove7.common.appbus.SpeechAction
import cn.vove7.common.model.RequestPermission
import cn.vove7.common.utils.gone
import cn.vove7.common.utils.runOnUi
import cn.vove7.common.utils.show
import cn.vove7.jarvis.R
import cn.vove7.vtp.log.Vog
import kotlinx.android.synthetic.main.toast_listening_text.view.*

/**
 * # FloatyPanel
 * 悬浮语音面板
 * @author 17719247306
 * 2018/9/9
 */
class FloatyPanel : AbFloatWindow(GlobalApp.APP) {
    override var posY: Int = 0

    //    lateinit var rootView: LinearLayout
    override fun layoutResId(): Int = R.layout.toast_listening_text

    override val onNoPermission: () -> Unit = {
        AppBus.postSpeechAction(SpeechAction.ActionCode.ACTION_CANCEL_RECOG)
        AppBus.post(RequestPermission("悬浮窗权限"))
    }

    fun showParseAni() {
        if (contentView?.parse_ani?.isShown == true) return
        contentView?.parse_ani?.apply {
            (drawable as? AnimationDrawable)?.start()
            show()
        }
        contentView?.listening_ani?.gone()
    }

    fun showListeningAni() {
        if (contentView?.listening_ani?.isShown == true) return
        contentView?.parse_ani?.gone()
        contentView?.listening_ani?.show()
    }

    override fun onCreateView(view: View) {
        view.body.setPadding(10, statusbarHeight + 15, 10, 15)
    }

    fun show(text: String) {
        removeDelayHide()
        runOnUi {
            show()
            showListeningAni()
            voiceText = text
            contentView?.voice_text?.text = text
        }
    }

    override val windowsAnimation: Int = R.style.FloatyWindow

    var voiceText = ""

    override fun afterShow() {
        contentView?.voice_text?.text = voiceText
    }


    private fun removeDelayHide() {
        delayHandler?.removeCallbacks(delayHide)
    }

    fun showAndHideDelay(text: String) {
        show(text)
        hideDelay(1000)
    }

    var delayHide = Runnable {
        hide()
    }

    var delayHandler: Handler? = null
    fun hideDelay(delay: Long = 800) {
        runOnUi {
            if (delayHandler == null) delayHandler = Handler()
            delayHandler?.postDelayed(delayHide, delay)
        }
        Vog.d("hide delay $delay")
    }

    fun hideImmediately() {//立即
        runOnUi {
            hide()
        }
    }

}
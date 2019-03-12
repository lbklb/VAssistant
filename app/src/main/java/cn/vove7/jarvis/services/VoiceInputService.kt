package cn.vove7.jarvis.services

import android.content.Intent
import android.speech.RecognitionService
import android.speech.SpeechRecognizer
import cn.vove7.common.app.GlobalApp
import cn.vove7.common.appbus.AppBus
import cn.vove7.common.model.VoiceRecogResult
import cn.vove7.vtp.builder.BundleBuilder
import cn.vove7.vtp.log.Vog
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * # VoiceInputService
 *
 * @author 11324
 * 2019/1/22
 */
class VoiceInputService : RecognitionService() {
    var listener: Callback? = null
    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        listener?.beginningOfSpeech()
        this.listener = listener
        Vog.d(this, "onStartListening ---> 开始")
        AppBus.reg(this)
        MainService.instance?.startVoiceInput() ?: let {
            GlobalApp.toastShort("App未就绪")
            stopSelf()
        }
    }

    override fun onCancel(listener: Callback?) {
        MainService.instance?.onCommand(AppBus.ORDER_CANCEL_RECOG)
        Vog.d(this, "onCancel ---> 取消")
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onResult(voiceResult: VoiceRecogResult) {
        val rs = arrayListOf(voiceResult.result)
        listener?.results(BundleBuilder().put(SpeechRecognizer.RESULTS_RECOGNITION, rs).data)
    }


    override fun onStopListening(listener: Callback?) {
        MainService.instance?.onCommand(AppBus.ORDER_STOP_RECOG)
        Vog.d(this, "onStopListening ---> 停止")
    }

}
package cn.vove7.jarvis.speech

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.support.annotation.CallSuper
import cn.vove7.common.app.GlobalApp
import cn.vove7.common.appbus.VoiceData
import cn.vove7.jarvis.BuildConfig
import cn.vove7.jarvis.receivers.PowerEventReceiver
import cn.vove7.jarvis.speech.baiduspeech.recognition.model.IStatus
import cn.vove7.jarvis.tools.AppConfig
import cn.vove7.vtp.log.Vog

/**
 * #SpeechRecoService
 * 语音识别接口
 *
 * Created by Administrator on 2018/11/4
 */
abstract class SpeechRecoService(val event: SpeechEvent) : SpeechRecoI {
    val context: Context
        get() = GlobalApp.APP

    abstract var enableOffline: Boolean

    var isListening = false
    @CallSuper
    override fun startRecog() {
        isListening = true
    }

    @CallSuper
    override fun cancelRecog(notify: Boolean) {
        isListening = false
    }

    @CallSuper
    override fun stopRecog() {
        isListening = false
    }

    /**
     * 定时关闭语音唤醒任务
     */
    private val stopWakeUpTimer = Runnable {
        stopWakeUp()
    }

    private val timerHandler: Handler by lazy {
        val t = HandlerThread("auto_sleep")
        t.start()
        Handler(t.looper)
    }

    /**
     * 开启定时关闭
     * 重启定时器
     */
    fun startAutoSleepWakeUp() {
        if (PowerEventReceiver.isCharging) return
        stopAutoSleepWakeup()
        Vog.d(this, "startAutoSleepWakeUp ---> 开启自动休眠")
        timerHandler.postDelayed(stopWakeUpTimer,
                if (BuildConfig.DEBUG) 60000
                else AppConfig.autoSleepWakeupMillis)
    }

    //关闭定时器
    fun stopAutoSleepWakeup() {
        Vog.d(this, "stopAutoSleepWakeup ---> 关闭自动休眠")
        timerHandler.removeCallbacks(stopWakeUpTimer)
    }


    /**
     * 事件分发[中枢]
     * @constructor
     */
    inner class RecoHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message?) {
            when (msg?.what) {
                IStatus.CODE_WAKEUP_SUCCESS -> {//唤醒
                    val word = msg.data.getString("data")
                    startAutoSleepWakeUp()
                    if (!event.onWakeup(word))
                        return
//                    AppBus.postVoiceData(VoiceData(msg.what, word))
                    cancelRecog(false)
                    startRecog()
                    return
                }
                IStatus.CODE_VOICE_TEMP -> {//中间结果
                    val res = msg.data.getString("data") ?: "null"
                    event.onTempResult(res)
//                    AppBus.postVoiceData(VoiceData(msg.what, res))
                }
                IStatus.CODE_VOICE_ERR -> {//出错
                    val res = msg.data.getString("data") ?: "null"
                    isListening = false
                    event.onFailed(res)
//                    AppBus.postVoiceData(VoiceData(msg.what, res))
                }
                IStatus.CODE_VOICE_VOL -> {//音量反馈
                    val data = msg.data.getSerializable("data") as VoiceData
                    event.onVolume(data)
//                    AppBus.postVoiceData(data)
                }
                IStatus.CODE_VOICE_RESULT -> {//结果
                    val result = msg.data.getString("data") ?: "null"
                    event.onResult(result)
//                    AppBus.postVoiceData(VoiceData(msg.what, result))
                }
            }
        }
    }
}

interface SpeechRecoI {
    val wakeupI: WakeupI
    fun startRecog()
    fun cancelRecog(notify: Boolean = true)
    fun startWakeUp()
    fun stopWakeUp()
    fun release()

    /**
     * 开始录音后，手动停止录音。SDK会识别在此过程中的录音。点击“停止”按钮后调用。
     */
    fun stopRecog()
}
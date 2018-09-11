package cn.vove7.jarvis.speech.wakeup

import android.os.Handler
import android.os.Message
import cn.vove7.common.app.GlobalApp

import cn.vove7.jarvis.speech.recognition.model.IStatus
import cn.vove7.vtp.builder.BundleBuilder


/**
 * Created by fujiayi on 2017/9/21.
 */

class RecogWakeupListener(private val handler: Handler) : SimpleWakeupListener(), IStatus {

    override fun onSuccess(word: String?, result: WakeUpResult) {
        super.onSuccess(word, result)
        val m = Message()
        m.what = IStatus.CODE_WAKEUP_SUCCESS
        m.data = BundleBuilder().put("data", word ?: "").build()
        handler.sendMessage(m)
    }

    override fun onStop() {
        super.onStop()
        GlobalApp.toastShort("语音唤醒已关闭")
    }
}

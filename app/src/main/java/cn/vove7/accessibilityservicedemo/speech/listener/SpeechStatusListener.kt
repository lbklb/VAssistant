package cn.vove7.accessibilityservicedemo.speech.listener

import android.os.Handler
import cn.vove7.accessibilityservicedemo.services.MainService.Companion.WHAT_VOICE_ERR
import cn.vove7.accessibilityservicedemo.services.MainService.Companion.WHAT_VOICE_RESULT
import cn.vove7.accessibilityservicedemo.services.MainService.Companion.WHAT_VOICE_TEMP
import cn.vove7.accessibilityservicedemo.services.MainService.Companion.WHAT_VOICE_VOL
import cn.vove7.accessibilityservicedemo.speech.message.SpeechMessage
import cn.vove7.accessibilityservicedemo.speech.model.RecogResult
import cn.vove7.appbus.VoiceData
import cn.vove7.vtp.log.Vog

/**
 *
 *运算器
 * Created by Vove on 2018/6/18
 */
class SpeechStatusListener(private val handler: Handler) : StatusRecogListener() {

    var isSuccess = false
    override fun onAsrBegin() {
        super.onAsrBegin()
        isSuccess = false
    }

    override fun onAsrPartialResult(results: Array<String>?, recogResult: RecogResult) {
        super.onAsrPartialResult(results, recogResult)
        val tmp = results?.get(0) ?: ""
        handler.sendMessage(SpeechMessage.buildMessage(WHAT_VOICE_TEMP, tmp))
    }

    override fun onAsrFinalResult(results: Array<String>?, recogResult: RecogResult) {
        super.onAsrFinalResult(results, recogResult)
        isSuccess = true
        val tmp = results?.get(0) ?: ""
        handler.sendMessage(SpeechMessage.buildMessage(WHAT_VOICE_RESULT, tmp))
    }

    override fun onAsrFinishError(errorCode: Int, subErrorCode: Int, errorMessage: String?, descMessage: String?,
                                  recogResult: RecogResult) {
        super.onAsrFinishError(errorCode, subErrorCode, errorMessage, descMessage, recogResult)
        val message = "识别错误, 错误码：$errorCode,$subErrorCode"
        handler.sendMessage(SpeechMessage.buildMessage(WHAT_VOICE_ERR, message))
    }

    override fun onAsrVolume(volumePercent: Int, volume: Int) {
        Vog.i(this, "音量百分比$volumePercent ; 音量$volume")
        handler.sendMessage(
                SpeechMessage.buildMessage(
                        WHAT_VOICE_VOL,
                        VoiceData(WHAT_VOICE_VOL, volumePercent = volumePercent)
                )
        )
    }

    override fun onAsrExit() {
        super.onAsrExit()
//        if (!isSuccess) {
//            Vog.d(this, "识别失败")
//            handler.sendMessage(SpeechMessage.buildMessage(WHAT_VOICE_ERR, "无结果"))
//        }
    }
}
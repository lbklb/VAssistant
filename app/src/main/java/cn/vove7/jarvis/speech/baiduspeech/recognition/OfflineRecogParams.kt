package cn.vove7.jarvis.speech.baiduspeech.recognition

import android.Manifest
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import cn.vove7.common.app.GlobalApp
import cn.vove7.common.helper.AdvanAppHelper
import cn.vove7.common.helper.AdvanContactHelper
import cn.vove7.jarvis.speech.baiduspeech.BaiduSpeechRecogService
import com.baidu.speech.asr.SpeechConstant


object OfflineRecogParams : CommonRecogParams() {

    fun fetchOfflineParams(): Map<String, Any> = mapOf(
            Pair(SpeechConstant.DECODER, 2),
            Pair(SpeechConstant.ASR_OFFLINE_ENGINE_GRAMMER_FILE_PATH, "asset:///bd/baidu_speech_grammar.bsg"),
            Pair(SpeechConstant.SLOT_DATA,
                    BaiduSpeechRecogService.OffWord(
                            if (ActivityCompat.checkSelfPermission(GlobalApp.APP,
                                            Manifest.permission.READ_CONTACTS)
                                    != PackageManager.PERMISSION_GRANTED) //首次启动无权限 不做
                                arrayOf() else AdvanContactHelper.getContactName(),
                            AdvanAppHelper.getAppName()
                    ).toString()
            )
    )

}


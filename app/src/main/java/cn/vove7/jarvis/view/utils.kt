package cn.vove7.jarvis.view

import android.content.Context
import cn.vove7.common.accessibility.AccessibilityApi
import cn.vove7.common.bridges.RootHelper
import cn.vove7.jarvis.tools.AppConfig
import cn.vove7.vtp.log.Vog
import cn.vove7.vtp.runtimepermission.PermissionUtils
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader

/**
 * # Utils
 *
 * @author Administrator
 * 2018/12/30
 */

/**
 * root自动开启 or 系统应用
 */
fun openAccessibilityServiceAuto(context: Context) {
    Vog.d("", "openAccessibilityService ---> 打开无障碍")
    if (PermissionUtils.accessibilityServiceEnabled(context)) return
    /*if (AppConfig.IS_SYS_APP) {
        Vog.d("", "openAccessibilityService ---> 打开无障碍 as SYS_APP")
        AccessibilityApi.openServiceSelf()
    } else */if (AppConfig.autoOpenASWithRoot) {
        Vog.d("", "openAccessibilityService ---> 打开无障碍 as su")
        RootHelper.openSelfAccessService()
    }
}

fun wirelessDebug(en: Boolean) {
    RootHelper.execWithSu("setprop service.adb.tcp.port ${if (en) "5555" else "-1"}\n" +
            "stop adbd\n" +
            "start adbd")
}

fun isWirelessDebugEnable(): Boolean {
    try {
        val proc = Runtime.getRuntime().exec("su")
        val os = DataOutputStream(proc.outputStream)
        os.writeBytes("getprop service.adb.tcp.port\n")
        os.flush()
        os.close()
        val reader = InputStreamReader(proc.inputStream)
        val chars = CharArray(5)
        reader.read(chars)
        reader.close()
        proc.destroy()
        val result = String(chars)
        return result.matches("[0-9]+\\n".toRegex()) && !result.contains("-1")
    } catch (e: IOException) {
        e.printStackTrace()
        return false
    }
}
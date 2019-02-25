package cn.vove7.jarvis.tools

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.Process
import cn.vove7.common.app.GlobalApp
import cn.vove7.common.app.GlobalLog
import cn.vove7.common.model.UserInfo
import cn.vove7.common.netacc.ApiUrls
import cn.vove7.common.netacc.NetHelper
import cn.vove7.common.netacc.model.BaseRequestModel
import cn.vove7.common.utils.TextHelper
import cn.vove7.jarvis.BuildConfig
import cn.vove7.jarvis.activities.CrashInfoActivity
import cn.vove7.vtp.sharedpreference.SpHelper
import cn.vove7.vtp.system.DeviceInfo
import cn.vove7.vtp.system.SystemHelper
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.lang.Thread.sleep
import java.text.SimpleDateFormat
import java.util.*

/**
 * # CrashHandler
 *
 * @author Administrator
 * 9/25/2018
 */
object CrashHandler : Thread.UncaughtExceptionHandler {
    // 系统默认的UncaughtException处理类
    private var mDefaultHandler: Thread.UncaughtExceptionHandler? = null

    val context: Context by lazy {
        GlobalApp.APP
    }

    fun init() {
        // 获取系统默认的UncaughtException处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        // 设置该CrashHandler为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(t: Thread?, e: Throwable?) {
        if (!handleException(e) && mDefaultHandler != null) {
            // 如果用户没有处理则让系统默认的异常处理器来处
            mDefaultHandler?.uncaughtException(t, e)
        } else {
            val sp = SpHelper(context)
            val lastCrashTime = sp.getLong("last_crash_time")
            val now = System.currentTimeMillis()
            sp.set("last_crash_time", now)
            sp.set("last_crash_data", SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()).format(Date()))
            if (now > lastCrashTime + 60 * 1000) {//restart
                val intent = Intent(context, CrashInfoActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
            try {
                sleep(2000)
            } catch (e: Exception) {
            }
            Process.killProcess(Process.myPid())
//            System.exit(0)// 关闭已奔溃的app进程
        }

    }

    /**
     * 自定义错误捕获
     *
     * @param e
     * @return true:如果处理了该异常信息;否则返回false.
     */
    private fun handleException(e: Throwable?): Boolean {
        if (e == null) {
            return false
        }
        val headerInfo = SystemHelper.getDeviceInfo(context).string()
        val log = GlobalLog.toString()
        val errFile = Environment.getExternalStorageDirectory().absolutePath + "/crash.log"
        try {
            val outFile = context.cacheDir.absolutePath + "/crash.log"
            PrintWriter(BufferedWriter(FileWriter(File(outFile)))).apply {
                println(headerInfo)
                println(SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                e.printStackTrace(this)
                println(log)
                close()
            }
            val info = File(outFile).readText()
            try {//输出和sd卡
                File(errFile).writeText(info)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (!BuildConfig.DEBUG)
                NetHelper.postJson<Any>(ApiUrls.CRASH_HANDLER, BaseRequestModel(info))
        } catch (e1: Exception) {//文件读写
            if (!BuildConfig.DEBUG)
                NetHelper.postJson<Any>(ApiUrls.CRASH_HANDLER,
                        BaseRequestModel(headerInfo + e.message + log + "crash上传失败${e1.message}"))
        }

        return true
    }
}

fun DeviceInfo.string(): String {
    return buildString {
        append("appId:").append(BuildConfig.APPLICATION_ID)
        append("userId: ").append(UserInfo.getUserId()).appendln()
        append("userName: ").append(UserInfo.getUserName()).appendln()
        append("email: ").append(UserInfo.getEmail()).appendln()
        append("appVersion: ").append(AppConfig.versionName).appendln()
        append("manufacturerName: ").append(manufacturerName).appendln()
        append("productName: ").append(productName).appendln()
        append("brandName: ").append(brandName).appendln()
        append("model: ").append(model).appendln()
        append("boardName: ").append(boardName).appendln()
        append("deviceName: ").append(deviceName).appendln()
        append("serial: ").append(serial).appendln()
        append("sdkInt: ").append(sdkInt).appendln()
        append("androidVersion: ").append(androidVersion).appendln()
        append("ABI  : ").append(TextHelper.arr2String(Build.SUPPORTED_ABIS)).appendln()
        append("运行时间：" + (System.currentTimeMillis() - GlobalApp.launchTime) / 1000 + "s")
    }
}
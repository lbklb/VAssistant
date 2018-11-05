package cn.vove7.jarvis.tools

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.Process
import android.widget.Toast
import cn.vove7.common.app.GlobalApp
import cn.vove7.common.app.GlobalLog
import cn.vove7.common.model.UserInfo
import cn.vove7.common.netacc.ApiUrls
import cn.vove7.common.netacc.NetHelper
import cn.vove7.common.netacc.model.BaseRequestModel
import cn.vove7.common.utils.TextHelper
import cn.vove7.jarvis.BuildConfig
import cn.vove7.jarvis.activities.CrashInfoActivity
import cn.vove7.jarvis.chat.Inten
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

    private val errFile = Environment.getExternalStorageDirectory().absolutePath + "/crash.log"
    override fun uncaughtException(t: Thread?, e: Throwable?) {
        if (!handleException(e) && mDefaultHandler != null) {
            // 如果用户没有处理则让系统默认的异常处理器来处
            mDefaultHandler?.uncaughtException(t, e)
        } else {
            val sp = SpHelper(context)
            val lastCrashTime = sp.getLong("last_crash_time")
            val now = System.currentTimeMillis()
            sp.set("last_crash_time", now)
            if (now > lastCrashTime + 60 * 1000) {//restart
                val intent = Intent(context, CrashInfoActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
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

        try {
            val pw = PrintWriter(BufferedWriter(FileWriter(File(errFile))))
            pw.println(headerInfo)
            pw.println(SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            e.printStackTrace(pw)
            pw.println(log)
            pw.close()
            if (!BuildConfig.DEBUG)
                NetHelper.postJson<Any>(ApiUrls.CRASH_HANDLER, BaseRequestModel(File(errFile).readText())) { _, _ -> }
        } catch (e1: Exception) {//文件读写
            //
            Toast.makeText(context, "写入错误记录失败，请给予读写存储权限", Toast.LENGTH_SHORT).show()
            if (!BuildConfig.DEBUG)
                NetHelper.postJson<Any>(ApiUrls.CRASH_HANDLER,
                        BaseRequestModel(headerInfo + e.message + log + "\n\n写入失败${e1.message}")) { _, _ -> }
        }

        return true
    }
}

fun DeviceInfo.string(): String {
    val b = StringBuilder()
    b.append("userId: ").append(UserInfo.getUserId()).appendln()
    b.append("userName: ").append(UserInfo.getUserName()).appendln()
    b.append("email: ").append(UserInfo.getEmail()).appendln()
    b.append("appVersion: ").append(AppConfig.versionName).appendln()
    b.append("manufacturerName: ").append(manufacturerName).appendln()
    b.append("productName: ").append(productName).appendln()
    b.append("brandName: ").append(brandName).appendln()
    b.append("model: ").append(model).appendln()
    b.append("boardName: ").append(boardName).appendln()
    b.append("deviceName: ").append(deviceName).appendln()
    b.append("serial: ").append(serial).appendln()
    b.append("sdkInt: ").append(sdkInt).appendln()
    b.append("androidVersion: ").append(androidVersion).appendln()
    b.append("ABI  : ").append(TextHelper.arr2String(Build.SUPPORTED_ABIS)).appendln()

    return b.toString()
}
package cn.vove7.jarvis

import android.app.ActivityManager
import android.content.Context
import android.support.v4.app.CoreComponentFactory
import cn.jpush.android.api.JPushInterface
import cn.vove7.androlua.LuaApp
import cn.vove7.common.app.AppConfig
import cn.vove7.common.app.GlobalApp
import cn.vove7.common.appbus.AppBus
import cn.vove7.common.helper.AdvanAppHelper
import cn.vove7.common.model.UserInfo
import cn.vove7.common.utils.ThreadPool.runOnPool
import cn.vove7.common.utils.runOnNewHandlerThread
import cn.vove7.common.utils.runWithClock
import cn.vove7.jarvis.droidplugin.RePluginManager
import cn.vove7.jarvis.receivers.AppInstallReceiver
import cn.vove7.jarvis.receivers.PowerEventReceiver
import cn.vove7.jarvis.receivers.ScreenStatusListener
import cn.vove7.jarvis.receivers.UtilEventReceiver
import cn.vove7.jarvis.services.MainService
import cn.vove7.jarvis.tools.AppNotification
import cn.vove7.jarvis.tools.CrashHandler
import cn.vove7.jarvis.tools.ShortcutUtil
import cn.vove7.jarvis.view.openAccessibilityServiceAuto
import cn.vove7.vtp.log.Vog
import com.umeng.commonsdk.UMConfigure
import com.wanjian.cockroach.Cockroach
import org.greenrobot.eventbus.Subscribe


@Suppress("MemberVisibilityCanBePrivate")
class App : GlobalApp() {

    private lateinit var mainService: MainService

    override fun onCreate() {
        super.onCreate()
        Vog.d("onCreate ---> begin ${System.currentTimeMillis() / 1000}")
        ins = this

        runWithClock("加载配置") {
            AppConfig.init()
        }

        if (!isMainProcess) {
            Vog.d("非主进程")
            return
        }
        AppBus.reg(this)

        Cockroach.install(CrashHandler)

        JPushInterface.setDebugMode(BuildConfig.DEBUG);
        JPushInterface.init(this)
        runOnNewHandlerThread("app_load") {
            if (AppConfig.FIRST_LAUNCH_NEW_VERSION || BuildConfig.DEBUG)
                LuaApp.init(this, AppConfig.FIRST_LAUNCH_NEW_VERSION)
            startServices()
            ShortcutUtil.initShortcut()
            AdvanAppHelper.getPkgList()
            startBroadcastReceivers()
            runOnPool {
                openAccessibilityServiceAuto()
            }
            RePluginManager().launchWithApp()
            Vog.d("onCreate ---> 结束 ${System.currentTimeMillis() / 1000}")
            System.gc()

            initUm()
        }

    }

    private fun startServices() {
        mainService = MainService()
    }

    private fun startBroadcastReceivers() {
        runOnNewHandlerThread("startBroadcastReceivers", delay = 5000) {
            PowerEventReceiver.start()
            ScreenStatusListener.start()
            AppInstallReceiver.start()
            UtilEventReceiver.start()
//            BTConnectListener.start()
        }
    }

    private fun stopBroadcastReceivers() {
        PowerEventReceiver.stop()
        ScreenStatusListener.stop()
        AppInstallReceiver.stop()
        UtilEventReceiver.stop()
    }


    companion object {
        var ins: App? = null

        fun startServices() {
            ins?.startServices()
        }
    }

    private fun initUm() {
        UMConfigure.init(this, BuildConfig.UM_KEY, "default", UMConfigure.DEVICE_TYPE_PHONE, "")
    }

    override fun onTerminate() {
        MainService.instance?.destroy()
        stopBroadcastReceivers()
        super.onTerminate()
    }

    /**
     * 是否为主进程
     * 未配置主进程名 默认为包名
     */
    val isMainProcess
        get() = this.packageName == currentProcessName

    val currentProcessName
        get(): String? {
            val pid = android.os.Process.myPid()
            val manager = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            manager.runningAppProcesses.forEach { process ->
                if (process.pid == pid)
                    return process.processName.also {
                        Vog.d("进程：${process.processName}")
                    }
            }
            return null
        }

    @Subscribe
    fun onUserInit(event: String) {
        if(event == AppBus.EVENT_USER_INIT) {
            JPushInterface.setAlias(this, UserInfo.getUserId().toString(), null)
        }
    }
}
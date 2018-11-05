package cn.vove7.jarvis.tools.debugserver

import android.os.Handler
import android.os.Looper
import cn.vove7.androlua.LuaHelper
import cn.vove7.common.app.GlobalApp
import cn.vove7.common.app.GlobalLog
import cn.vove7.common.appbus.AppBus
import cn.vove7.common.datamanager.parse.model.Action
import cn.vove7.common.executor.OnPrint
import cn.vove7.jarvis.BuildConfig
import cn.vove7.jarvis.R
import cn.vove7.jarvis.services.MainService
import cn.vove7.jarvis.tools.AppConfig
import cn.vove7.rhino.api.RhinoApi
import cn.vove7.vtp.log.Vog
import com.google.gson.Gson
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread


object RemoteDebugServer : Runnable {

    //    companion object {
    var server: ServerSocket? = null

//    var out: MutableList<PrintWriter>? = null

    var clients: MutableList<Pair<Socket, PrintWriter>>? = null
    var stopped: Boolean = true
    private const val LISTEN_PORT = 1527
    var thread: Thread? = null

    var handler: Handler? = null
    fun start() {
        if (!AppConfig.checkUser()) {
            return
        }
        if (!stopped && thread?.isAlive == true) {
            Vog.d(this, "start ---> thread is Alive")
            return
        }
        thread = thread {
            handler = Handler(Looper.getMainLooper())
            startAutoSleep()
            RemoteDebugServer.run()
        }

    }

    fun stop() {
        thread {
            stopped = true
            server?.close()
            clients?.forEach {
                try {
                    it.first.close()
                    it.second.close()
                } catch (e: Exception) {
                }
            }
            clients?.clear()

            thread?.interrupt()

            stopAutoSleep()
            server = null
            thread = null
        }
    }
//    }

    override fun run() {
        stopped = false
        clients = mutableListOf()
        server = ServerSocket(LISTEN_PORT)
        RhinoApi.regPrint(print)
        LuaHelper.regPrint(print)
        GlobalApp.toastShort(GlobalApp.getString(R.string.text_debug_service_starting))
        server.use {
            while (!stopped) {
                try {
                    val client = server!!.accept()//等待
                    stopAutoSleep()
                    val inputStream = BufferedReader(InputStreamReader(client!!.getInputStream(), "UTF-8"))
                    val o = PrintWriter(BufferedWriter(OutputStreamWriter(client.getOutputStream())), true)
                    val p = Pair(client, o)
                    clients?.add(p)
                    GlobalApp.toastShort(String.format(GlobalApp.getString(R.string.text_establish_connection), client.inetAddress
                        ?: "none"))
                    print.onPrint(0, "连接成功 ${client.inetAddress}")
                    //type -> script -> arg
                    thread {
                        try {
                            while (!stopped) {
                                val data = inputStream.readLine()
                                if (data == null) {//断开连接
//                                    onDisConnect(client)
                                    break
                                } else onPostAction(data)
                            }
                        } catch (e: Exception) {//
                            GlobalLog.err(e)
                        }
                        onDisConnect(p)
                    }
                } catch (e: Exception) {
                    GlobalApp.toastShort(GlobalApp.getString(R.string.text_disconnect_with_debugger))
                }
            }
        }
        show("RemoteDebug finished!")
// finally {
        stopped = true
        onFinish()
//        }
    }

    private val sleepTime = if (BuildConfig.DEBUG) 100000L else 300000L //5min
    private val sleepRun = Runnable {
        Vog.d(this, "sleep ---> 休眠")
        stop()
    }

    private fun startAutoSleep() {
        Vog.d(this, "startAutoSleep ---> 开启自动休眠$sleepTime")
        handler?.postDelayed(sleepRun, sleepTime)
    }

    private fun stopAutoSleep() {
        Vog.d(this, "stopAutoSleepWakeup ---> 关闭自动休眠")

        handler?.removeCallbacks(sleepRun)
    }

    private fun onFinish() {
        RhinoApi.unregPrint(print)
        LuaHelper.unRegPrint(print)
    }

    private val print = object : OnPrint {
        override fun onPrint(l: Int, output: String) {
//            Vog.d(this, "onPrint ---> $output")

            val end = if (output.endsWith('\n')) "" else "\n"
            try {
                clients?.forEach {
                    it.second.print(output + end)
                    it.second.flush()
                }
            } catch (e: Exception) {
                e.printStackTrace()
//                onFinish()
            }
        }
    }

    private fun onDisConnect(client: Pair<Socket, PrintWriter>) {
        client.first.apply {
            GlobalApp.toastShort("与${inetAddress}断开连接")
            close()
        }
        clients?.remove(client)
        if (clients?.isEmpty() == true)
            startAutoSleep()
    }

    /**
     * 解析动作
     * @param actionJson String
     */
    private fun onPostAction(actionJson: String) {
        Vog.d(this, "onPostAction ---> $actionJson")

        thread {
            val action: RemoteAction
            try {
                action = Gson().fromJson<RemoteAction>(actionJson, RemoteAction::class.java)
            } catch (e: Exception) {
                GlobalLog.err(e)
                print.onPrint(0, "发生错误${e.message}")
                return@thread
            }
            when (action.action) {
                "run" -> {
                    val ac = Action()
                    ac.actionScript = action.text
                    when (action.type) {
                        "lua" -> ac.scriptType = Action.SCRIPT_TYPE_LUA
                        "javascript" -> ac.scriptType = Action.SCRIPT_TYPE_JS
                        else -> print.onPrint(0, "不支持的语言${action.type}")
                    }
                    AppBus.post(ac)
                }
                "stop" -> {
                    AppBus.post(AppBus.ORDER_STOP_EXEC)
                }
                "command" -> {//文本指令
                    val cmd = action.text
                    if (cmd == null) {
                        print.onPrint(0, "无动作")
                    } else {
                        print.onPrint(0, "执行：$cmd")
                        MainService.instance?.onParseCommand(cmd)
                    }

                }
            }
        }
    }

    private fun show(s: String) {
        Vog.d(this, "show  ----> $s")
    }

}

class RemoteAction(
        val action: String,
        val type: String?,
        val text: String?
)
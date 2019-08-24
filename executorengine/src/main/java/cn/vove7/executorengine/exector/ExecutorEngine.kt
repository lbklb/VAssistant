package cn.vove7.executorengine.exector

import cn.vove7.androlua.LuaHelper
import cn.vove7.common.BridgeManager
import cn.vove7.common.NeedAccessibilityException
import cn.vove7.common.NotSupportException
import cn.vove7.common.app.GlobalLog
import cn.vove7.common.bridges.GlobalActionExecutor
import cn.vove7.common.bridges.SystemBridge
import cn.vove7.common.datamanager.parse.model.Action
import cn.vove7.common.executor.CExecutorI.Companion.EXEC_CODE_FAILED
import cn.vove7.common.executor.CExecutorI.Companion.EXEC_CODE_INTERRUPT
import cn.vove7.common.executor.CExecutorI.Companion.EXEC_CODE_NOT_SUPPORT
import cn.vove7.common.executor.CExecutorI.Companion.EXEC_CODE_REQUIRE
import cn.vove7.common.executor.CExecutorI.Companion.EXEC_CODE_SUCCESS
import cn.vove7.common.executor.OnPrint
import cn.vove7.common.executor.PartialResult
import cn.vove7.common.interfaces.ScriptEngine
import cn.vove7.common.utils.RegUtils
import cn.vove7.executorengine.ExecutorImpl
import cn.vove7.rhino.RhinoHelper
import cn.vove7.rhino.api.RhinoApi
import cn.vove7.smartkey.tool.Vog
import java.util.concurrent.ConcurrentSkipListSet

/**
 * # MultiExecutorEngine
 *
 * @author 17719247306
 * 2018/8/28
 */
class ExecutorEngine : ExecutorImpl() {
    private val bridgeManager = BridgeManager(this, GlobalActionExecutor,
            SystemBridge, serviceBridge)


    override fun onRhinoExec(script: String, argMap: Map<String, Any?>?): Pair<Int, String?> {

        val rhinoHelper = RhinoHelper(bridgeManager)
        engines.add(rhinoHelper)
        return runScriptWithCatch({
            rhinoHelper.evalString(script, argMap)
            RhinoApi.doLog("主线程执行完毕\n")
        },{
            RhinoApi.doLog(it)
        })
    }

    //didn't work fixme
    fun runActionSilent(action: Action, argMap: Map<String, Any?>?) {//静默
        currentAction = action
        runScript(action.actionScript, argMap)
    }

    /**
     * 执行器
     */
    private val engines = ConcurrentSkipListSet<ScriptEngine>()

    //可提取ExecutorHelper 接口 handleMessage
    override fun onLuaExec(script: String, argMap: Map<String, Any?>?): Pair<Int, String?> {
        val luaHelper = LuaHelper(context, bridgeManager)
        engines.add(luaHelper)
        val newScript = RegUtils.replaceLuaHeader(script)

        return runScriptWithCatch({
            luaHelper.evalString(newScript, argMap)
            luaHelper.handleMessage(OnPrint.INFO, "主线程执行完毕\n")
        }, {
            luaHelper.handleMessage(OnPrint.ERROR, it)
        })
    }

    private fun runScriptWithCatch(block: () -> Unit, onHandleErr: (String) -> Unit): Pair<Int, String?> {
        return try {
            block()
            EXEC_CODE_SUCCESS to null
        } catch (e: NotSupportException) {
            EXEC_CODE_NOT_SUPPORT to null
        } catch (e: NeedAccessibilityException) {
            EXEC_CODE_REQUIRE to e.message
        } catch (e: Throwable) {
            onHandleErr(e.message ?: "")
            GlobalLog.err(e)
            EXEC_CODE_FAILED to e.message
        }
    }


    override fun interrupt() {
        super.interrupt()
        release()
    }

    @Synchronized
    private fun release() {
        engines.forEach {
            it.release()
        }
        engines.clear()
    }

    override fun onFinish(resultCode: Int) {
        super.onFinish(resultCode)
        release()
    }
}
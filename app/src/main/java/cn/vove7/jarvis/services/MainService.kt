package cn.vove7.jarvis.services

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import cn.vove7.androlua.luautils.LuaContext
import cn.vove7.common.accessibility.AccessibilityApi
import cn.vove7.common.app.GlobalApp
import cn.vove7.common.app.GlobalLog
import cn.vove7.common.appbus.AppBus
import cn.vove7.common.appbus.AppBus.EVENT_BEGIN_SCREEN_PICKER
import cn.vove7.common.appbus.AppBus.EVENT_FORCE_OFFLINE
import cn.vove7.common.appbus.AppBus.EVENT_START_DEBUG_SERVER
import cn.vove7.common.appbus.AppBus.EVENT_STOP_DEBUG_SERVER
import cn.vove7.common.appbus.AppBus.ORDER_CANCEL_RECO
import cn.vove7.common.appbus.AppBus.ORDER_START_RECO
import cn.vove7.common.appbus.AppBus.ORDER_STOP_EXEC
import cn.vove7.common.appbus.AppBus.ORDER_STOP_RECO
import cn.vove7.common.appbus.SpeechAction
import cn.vove7.common.appbus.VoiceData
import cn.vove7.common.bridges.ChoiceData
import cn.vove7.common.bridges.ServiceBridge
import cn.vove7.common.bridges.ShowAlertEvent
import cn.vove7.common.bridges.ShowDialogEvent
import cn.vove7.common.datamanager.history.CommandHistory
import cn.vove7.common.datamanager.parse.model.Action
import cn.vove7.common.executor.CExecutorI
import cn.vove7.common.interfaces.SpeakCallback
import cn.vove7.common.model.RequestPermission
import cn.vove7.common.model.ResultBox
import cn.vove7.common.model.UserInfo
import cn.vove7.common.netacc.NetHelper
import cn.vove7.common.utils.RegUtils.checkCancel
import cn.vove7.common.utils.RegUtils.checkConfirm
import cn.vove7.executorengine.bridges.SystemBridge
import cn.vove7.executorengine.exector.MultiExecutorEngine
import cn.vove7.executorengine.parse.ParseEngine
import cn.vove7.jarvis.App
import cn.vove7.jarvis.R
import cn.vove7.jarvis.activities.PermissionManagerActivity
import cn.vove7.jarvis.activities.ScreenPickerActivity
import cn.vove7.jarvis.utils.AppConfig
import cn.vove7.jarvis.utils.debugserver.RemoteDebugServer
import cn.vove7.jarvis.view.dialog.MultiChoiceDialog
import cn.vove7.jarvis.view.dialog.OnMultiSelectListener
import cn.vove7.jarvis.view.dialog.OnSelectListener
import cn.vove7.jarvis.view.dialog.SingleChoiceDialog
import cn.vove7.jarvis.view.floatwindows.ListeningToast
import cn.vove7.jarvis.view.statusbar.ExecuteAnimation
import cn.vove7.jarvis.view.statusbar.ListeningAnimation
import cn.vove7.jarvis.view.statusbar.ParseAnimation
import cn.vove7.vtp.dialog.DialogUtil
import cn.vove7.vtp.log.Vog
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.SubscriberExceptionEvent
import org.greenrobot.eventbus.ThreadMode
import java.lang.Thread.sleep
import java.util.*
import kotlin.concurrent.thread

/**
 * 主服务
 */
class MainService : BusService(),
        ServiceBridge, OnSelectListener, OnMultiSelectListener, LuaContext {

    lateinit var listeningToast: ListeningToast
    override val serviceId: Int
        get() = 126
    /**
     * 悬浮窗
     */
//    private lateinit var floatVoice: VoiceFloat

    /**
     * 信使Action
     */
    var messengerAction: Action? = null
    /**
     * 执行器
     */
    private lateinit var cExecutor: CExecutorI

    private val speechRecoService = SpeechRecoService(RecgEventListener())
    private val speechSynService = SpeechSynService

    /**
     * 当前语音使用方式
     */
    private var voiceMode = MODE_VOICE
    //识别过程动画
    private var listeningAni = ListeningAnimation()
    private var parseAnimation = ParseAnimation()
    private var executeAnimation = ExecuteAnimation()

    override fun onCreate() {
        super.onCreate()
        instance = this
        GlobalApp.serviceBridge = this
        listeningToast = ListeningToast()
        cExecutor = MultiExecutorEngine()

//        floatVoice = VoiceFloat(this)
//        floatVoice.show()
        init()
    }

    fun init() {
        speechSynService.event = SyncEventListener()
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onSubException(e: SubscriberExceptionEvent) {
        Vog.d(this, "onSubException ---> $e")
        e.throwable?.printStackTrace()
        GlobalLog.err(e.throwable)
    }

    /**
     * 继续执行确认框
     */
    private var alertDialog: AlertDialog? = null

    @Subscribe(threadMode = ThreadMode.MAIN)
    override fun showAlert(r: ShowAlertEvent) {
        messengerAction = r.action
        alertDialog = AlertDialog.Builder(this)
                .setTitle(r.title)
                .setMessage(r.msg)
                .setCancelable(false)
                .setPositiveButton(R.string.text_continue) { _, _ ->
                    r.action.responseResult = true
                    notifyAlertResult()
                }.setNegativeButton(R.string.text_cancel) { _, _ ->
                    r.action.responseResult = false
                    notifyAlertResult()
                }.create()
        try {
            DialogUtil.setFloat(alertDialog!!)
            alertDialog?.show()
            //语音控制
            if (AppConfig.voiceControlDialog) {
                voiceMode = MODE_ALERT
                speechRecoService.startRecog()
            }
        } catch (e: Exception) {
            GlobalLog.err(e)
            onRequestPermission(RequestPermission("悬浮窗权限"))
            r.action.responseResult = false
            notifyAlertResult()
        }
    }

    /**
     * Alert同步
     * 停止语音
     */
    private fun notifyAlertResult() {
        if (AppConfig.voiceControlDialog) {
            speechRecoService.cancelRecog()
        }
        voiceMode = MODE_VOICE
        cExecutor.notifySync()
    }

    /**
     * 选择框
     */
    private var choiceDialog: Dialog? = null

    private fun hideDialog() {
        listeningAni.hideDelay()
        if (choiceDialog?.isShowing == true) {
            choiceDialog?.dismiss()
            choiceDialog = null
        }
    }


    /**
     * 选择对话框
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    override fun showChoiceDialog(event: ShowDialogEvent) {
        messengerAction = event.action
        choiceDialog = when (event.whichDialog) {
            ShowDialogEvent.WHICH_SINGLE -> {
                SingleChoiceDialog(this, event.askTitle, event.choiceDataSet, this)
            }
            ShowDialogEvent.WHICH_MULTI -> {
                MultiChoiceDialog(this, event.askTitle, event.choiceDataSet, this)
            }
            else -> return
        }.show()
    }

    /**
     * 单选回调
     */
    override fun onSingleSelect(pos: Int, data: ChoiceData?, msg: String) {
        Vog.d(this, "单选回调 $data")
        messengerAction?.responseResult = data != null
        messengerAction?.responseBundle?.putSerializable("data", data)
        messengerAction?.responseBundle?.putString("errMsg", msg)
        hideDialog()
        cExecutor.notifySync()
    }


    /**
     * 多选回调
     */
    override fun onMultiSelect(data: List<ChoiceData>?, msg: String) {
        messengerAction?.responseResult = data != null
        Vog.d(this, "多选回调 $data")
//        messengerAction?.responseBundle?.putSerializable("data", data)
        hideDialog()
        cExecutor.notifySync()
    }

    /**
     * 中途获取参数
     * @param action 执行动作
     */
    override fun getVoiceParam(action: Action) {
//        toast.showShort(action.param?.askText ?: "???")
        messengerAction = action
        voiceMode = MODE_GET_PARAM
        speechRecoService.startRecog()
    }

    private var speakSync = false
    override fun speak(text: String?) {
        //关闭语音播报 toast
        if (AppConfig.audioSpeak && SystemBridge.musicCurrentVolume != 0) {
            speakSync = false
            speechSynService.speak(text)
        } else {
            GlobalApp.toastShort(text ?: "null")
        }
    }

    private val speakCallbacks = mutableListOf<SpeakCallback>()

    private fun speakWithCallback(text: String?, call: SpeakCallback) {
        speakCallbacks.add(call)
        speakSync = true
        speechSynService.speak(text)
    }

    override fun speakSync(text: String?) {
        if (AppConfig.audioSpeak && SystemBridge.musicCurrentVolume != 0) {
            speakSync = true
            speechSynService.speak(text)
        } else {
            GlobalApp.toastShort(text ?: "null")
            cExecutor.speakCallback()
        }
    }

    override fun onExecuteStart(tag: String) {//
        Vog.d(this, "开始执行 -> $tag")
//        listeningToast.showAndHideDelay("开始执行")
        executeAnimation.begin()
    }

    /**
     * 执行结果回调
     */
    //from executor 线程

    override fun onExecuteFinished(result: String) {//
        Vog.d(this, result)

//        listeningToast.showAndHideDelay("执行完毕")
//        effectHandler.sendEmptyMessage(ANI_HIDEEND)
        executeAnimation.hideDelay()
//        toast.showShort(result)
    }

    //from executor 线程
    override fun onExecuteFailed(errMsg: String?) {//
        Vog.e(this, "onExecuteFailed: $errMsg")
        executeAnimation.failed()
        GlobalApp.toastShort(errMsg ?: "失败")
    }

    override fun onExecuteInterrupt(errMsg: String) {
        Vog.e(this, "onExecuteInterrupt: $errMsg")
        executeAnimation.failed()
        GlobalApp.toastShort("☹")
        executeAnimation.failed
    }

    override fun onBind(intent: Intent): IBinder {
        return object : Binder() {}
    }

    /**
     * onSpeechAction
     */
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onSpeechAction(sAction: SpeechAction) {
        when (sAction.action) {
            SpeechAction.ActionCode.ACTION_START_RECO -> {
                if (speechSynService.speaking) speechSynService.stop()
                speechRecoService.startRecog()
            }
            SpeechAction.ActionCode.ACTION_STOP_RECO -> speechRecoService.stopRecog()
            SpeechAction.ActionCode.ACTION_CANCEL_RECO -> {
                hideAll()
                speechRecoService.cancelRecog()
            }
            SpeechAction.ActionCode.ACTION_START_WAKEUP -> speechRecoService.startWakeUp()
            SpeechAction.ActionCode.ACTION_RELOAD_SYN_CONF -> speechSynService.reLoad()
            SpeechAction.ActionCode.ACTION_STOP_WAKEUP -> speechRecoService.stopWakeUp()
            else -> {
                Vog.e(this, sAction)
            }
        }
    }

    /**
     * 测试文本
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    fun runActionQue(que: PriorityQueue<Action>) {
        runActionQue(CExecutorI.DEBUG_SCRIPT, que)
    }

    fun runActionQue(cmd: String, que: PriorityQueue<Action>) {
        cExecutor.execQueue(cmd, que)
    }

    /**
     * 测试脚本
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    fun runAction(ac: Action) {
        val q = PriorityQueue<Action>()
        q.add(ac)
        cExecutor.execQueue(CExecutorI.DEBUG_SCRIPT, q)
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onCommand(order: String) {
        thread {
            when (order) {
                ORDER_STOP_EXEC -> {
                    speechRecoService.cancelRecog()
                    speechSynService.stop()
                    cExecutor.interrupt()
                    hideAll()
                }
                ORDER_STOP_RECO -> {
                    speechRecoService.stopRecog()
                }
                ORDER_CANCEL_RECO -> {
                    speechRecoService.cancelRecog()
                }
                ORDER_START_RECO -> {
                    speechRecoService.startRecog()
                }
                EVENT_FORCE_OFFLINE -> {
                    AppConfig.logout()
                }
                EVENT_START_DEBUG_SERVER -> {
                    RemoteDebugServer.start()
                }
                EVENT_STOP_DEBUG_SERVER -> {
                    RemoteDebugServer.stop()
                }
                EVENT_BEGIN_SCREEN_PICKER -> {
                    val intent = Intent(this, ScreenPickerActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    startActivity(intent)
                }
                else -> {
                }
            }
        }
    }

    /**
     * 请求权限
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    fun onRequestPermission(r: RequestPermission) {
        val intent = Intent(this, PermissionManagerActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("pName", r.permissionName)
        startActivity(intent)
    }

    override fun onDestroy() {
        speechRecoService.release()
        speechSynService.release()
        super.onDestroy()
    }

    companion object {
        /**
         * 正常语音模式
         */
        const val MODE_VOICE = 858
        /**
         * 执行期间获取"语音参数"
         */
        const val MODE_GET_PARAM = 72
        /**
         * 确认对话框语音模式
         */
        const val MODE_ALERT = 27

        /**
         * 语音事件数据类型
         */

        private val data = HashMap<String, Any>()
        var instance: MainService? = null

        val recoIsListening: Boolean
            get() {
                return (if (instance == null) {
                    thread {
                        GlobalApp.toastShort("正在启动服务")
                        App.startServices()
                        Vog.i(this, "instance ---> null")
                    }
                    false
                } else instance!!.speechRecoService.isListening()
                        ).also {
                    Vog.i(this, "recoIsListening ---> $it")
                }
            }
        val exEngineRunning: Boolean
            get() {
                return (if (instance == null) {
                    Vog.i(this, "instance ---> null")
                    false
                } else instance!!.cExecutor.running
                        ).also {
                    Vog.i(this, "exEngineRunning ---> $it")
                }
            }
    }

    private fun hideAll() {
        listeningToast.hideImmediately()
        listeningAni.hideDelay(0)
    }

    //识别前是否有音乐播放
    var haveMusicPlay = false
    //

    fun resumeMusicIf() {
        synchronized(haveMusicPlay) {
            if (haveMusicPlay) {
                SystemBridge.mediaResume()
                haveMusicPlay = false
            }
        }
    }

    override fun getGlobalData(): Map<*, *> {
        return data
    }

    override fun get(name: String): Any? {
        return data[name]
    }

    override fun getContext(): Context {
        return this
    }

    override fun call(name: String, args: Array<Any>) {}

    override fun set(name: String, `object`: Any) {
        data[name] = `object`
    }

    override fun getWidth(): Int {
        return resources.displayMetrics.widthPixels
    }

    override fun getHeight(): Int {
        return resources.displayMetrics.heightPixels
    }


    /**
     * 语音识别事件监听
     */
    inner class RecgEventListener : SpeechEvent {
        override fun onWakeup(word: String?) {
            if (AppConfig.openResponseWord && AppConfig.speakResponseWordOnVoiceWakeup) {
                speakResponseWord()
            }
        }

        private fun speakResponseWord() {
            Vog.d(this, "onStartRecog 响应词---> ${AppConfig.responseWord}")
            val resultBox = ResultBox<Boolean>()
            speakWithCallback(AppConfig.responseWord, object : SpeakCallback {
                override fun speakCallback(result: String?) {
                    Vog.d(this, "speakWithCallback ---> $result")
                    resultBox.setAndNotify(true)
                }
            })
            resultBox.blockedGet()
            Vog.d(this, "onStartRecog ---> speak finish")
            sleep(200)
        }

        override fun onStartRecog() {
            listeningAni.begin()//
            //todo 音效
            if (AppConfig.openResponseWord && !AppConfig.speakResponseWordOnVoiceWakeup) {
                speakResponseWord()
            }
            listeningToast.show("开始聆听")
            if (AppConfig.vibrateWhenStartReco) {
                SystemBridge.vibrate(80L)
            }

            checkMusic()
            if (!speechSynService.speaking) {
                speechSynService.stop()
            }
//            if (SystemBridge.isMediaPlaying() && ) {//防止误判合成服务播报
//                SystemBridge.mediaPause()
//                haveMusicPlay = true
//            }
        }

        override fun onResult(result: String) {//解析完成再 resumeMusicIf()?
            listeningToast.showAndHideDelay(result)

            Vog.d(this, "结果 --------> $result")
            when (voiceMode) {
                MODE_VOICE -> {
                    onParseCommand(result)
                }
                MODE_GET_PARAM -> {//中途参数
                    resumeMusicIf()
                    if (result == "") {//失败
                        //询问重新
//                            return
                        messengerAction?.responseResult = false
                        cExecutor.onGetVoiceParam(null)
                    } else {//通知
                        cExecutor.onGetVoiceParam(result)
                    }
                    voiceMode = MODE_VOICE
                }
                MODE_ALERT -> {
                    when {
                        checkConfirm(result) -> {
                            resumeMusicIf()
                            alertDialog?.getButton(DialogInterface.BUTTON_POSITIVE)?.performClick()
                            alertDialog = null
                            voiceMode = MODE_VOICE
                            executeAnimation.begin()//继续显示执行
                        }
                        checkCancel(result) -> {
                            resumeMusicIf()
                            alertDialog?.getButton(DialogInterface.BUTTON_NEGATIVE)?.performClick()
                            alertDialog = null
                            voiceMode = MODE_VOICE
                            executeAnimation.begin()
                        }
                        else -> AppBus.postSpeechAction(SpeechAction.ActionCode.ACTION_START_RECO)  //继续????
                    }
                }
            }
        }

        override fun onTempResult(temp: String) {
            listeningToast.show(temp)
            listeningAni.setContent(temp)
        }

        override fun onStop() {
            resumeMusicIf()
            listeningToast.hideDelay()
            parseAnimation.begin()
        }

        override fun onCancel() {
            Vog.d(this, "onCancel ---> ")
            resumeMusicIf()
            hideAll()
            if (voiceMode == MODE_GET_PARAM) {
                cExecutor.onGetVoiceParam(null)
                executeAnimation.begin()//continue
            } else if (voiceMode == MODE_ALERT) {
                //do nothing
            }
            voiceMode = MODE_VOICE
        }

        override fun onFailed(err: String) {
            resumeMusicIf()
            listeningToast.showAndHideDelay("😭")
            when (voiceMode) {
                MODE_VOICE -> {
                    listeningAni.failed()
                    hideAll()
                }
                MODE_GET_PARAM -> {//获取参数失败
                    cExecutor.onGetVoiceParam(null)
                    voiceMode = MODE_VOICE
                    executeAnimation.begin()//continue
                }
                MODE_ALERT -> {
//                        toast.showShort("重新说")
//                        speakSync("reSay")
                    AppBus.postSpeechAction(SpeechAction.ActionCode.ACTION_START_RECO)  //继续????
                }
            }
        }

        override fun onVolume(data: VoiceData) {
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun upHis(his: CommandHistory) {
        NetHelper.uploadUserCommandHistory(his)
    }

    /**
     * 解析指令
     * @param result String
     */
    fun onParseCommand(result: String) {
        hideAll()
        parseAnimation.begin()
        resumeMusicIf()
//        if (UserInfo.isVip() && AppConfig.onlyCloudServiceParse) {//高级用户且仅云解析
//            Vog.d(this, "onParseCommand ---> only云解析")
//            NetHelper.cloudParse(result) {
//                runFromCloud(result, it)
//            }
//            return
//        }
        val parseResult = ParseEngine
                .parseAction(result, AccessibilityApi.accessibilityService?.currentScope)
        if (parseResult.isSuccess) {
            val his = CommandHistory(UserInfo.getUserId(), result,
                    parseResult.msg)
            NetHelper.uploadUserCommandHistory(his)
            cExecutor.execQueue(result, parseResult.actionQueue)
        } else {// statistics
            //云解析
            if (AppConfig.cloudServiceParseIfLocalFailed) {
                Vog.d(this, "onParseCommand ---> 失败云解析")
                NetHelper.cloudParse(result) {
                    runFromCloud(result, it)
                }
            } else {
                if (UserInfo.isLogin()) {
                    NetHelper.uploadUserCommandHistory(CommandHistory(UserInfo.getUserId(), result, null))
                    listeningToast.showAndHideDelay("解析失败")
                    parseAnimation.failed()
                } else {
                    listeningToast.show("可能需要登陆同步下指令数据")
                    listeningToast.hideDelay(3000)
                    parseAnimation.hideDelay(0)
                }
            }
        }
    }

    private fun runFromCloud(command: String, actions: List<Action>?) {
        if (actions == null) {
            listeningToast.showAndHideDelay("解析失败")
            parseAnimation.failed()
            return
        }
        val que = PriorityQueue<Action>()
        actions.withIndex().forEach {
            it.value.priority = it.index
            que.add(it.value)
        }
        parseAnimation.finish()
        runActionQue(command, que)
    }

    /**
     * 语音合成事件监听
     */
    inner class SyncEventListener : SyncEvent {

        override fun onError(err: String, requestText: String?) {
            GlobalApp.toastShort(requestText ?: "")
            GlobalLog.err(err)
            if (speakSync) {
                cExecutor.speakCallback(err)
                speakCallbacks.forEach {
                    it.speakCallback(err)
                }
                speakCallbacks.clear()
            }
            resumeMusicIf()
        }

        override fun onFinish() {
            Vog.d(this, "onSynData 结束")
            if (speakSync) {
                cExecutor.speakCallback()
                speakCallbacks.forEach {
                    it.speakCallback()
                }
                speakCallbacks.clear()
            }
            resumeMusicIf()
        }

        override fun onStart() {
            Vog.d(this, "onSynData 开始")
            checkMusic()
        }
    }

    fun checkMusic() {
        if (SystemBridge.isMediaPlaying()) {
            SystemBridge.mediaPause()
            Vog.d(this, "checkMusic ---> 有音乐播放")
            haveMusicPlay = true
        } else {
            haveMusicPlay = false
            Vog.d(this, "checkMusic ---> 无音乐播放")
        }
    }
}


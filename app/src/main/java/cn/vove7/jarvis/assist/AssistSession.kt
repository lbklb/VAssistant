package cn.vove7.jarvis.assist

import android.app.AlertDialog
import android.app.Dialog
import android.app.assist.AssistContent
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.service.voice.VoiceInteractionSession
import android.support.annotation.RequiresApi
import android.support.design.widget.BottomSheetBehavior
import android.view.View
import android.widget.ProgressBar
import cn.vove7.common.appbus.AppBus
import cn.vove7.common.appbus.AppBus.EVENT_BEGIN_RECO
import cn.vove7.common.appbus.AppBus.EVENT_ERROR_RECO
import cn.vove7.common.appbus.AppBus.EVENT_FINISH_RECO
import cn.vove7.common.appbus.AppBus.EVENT_HIDE_FLOAT

import cn.vove7.common.bridges.UtilBridge
import cn.vove7.jarvis.services.MainService
import cn.vove7.jarvis.R
import cn.vove7.jarvis.adapters.SimpleListAdapter
import cn.vove7.jarvis.adapters.ViewModel
import cn.vove7.jarvis.view.BottomSheetController
import cn.vove7.vtp.log.Vog
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlin.concurrent.thread
import cn.vove7.common.app.GlobalApp
import cn.vove7.common.appbus.AppBus.ORDER_BEGIN_SCREEN_PICKER
import cn.vove7.jarvis.tools.baiduaip.BaiduAipHelper
import cn.vove7.common.model.RequestPermission
import cn.vove7.common.model.UserInfo
import cn.vove7.common.utils.runOnUi
import cn.vove7.executorengine.bridges.SystemBridge
import cn.vove7.jarvis.tools.QRTools
import cn.vove7.jarvis.view.dialog.ImageClassifyResultDialog
import cn.vove7.vtp.dialog.DialogUtil
import java.lang.Thread.sleep
import android.graphics.Matrix
import android.net.Uri
import android.os.*
import cn.vove7.common.utils.TextHelper
import cn.vove7.common.utils.formatNow
import cn.vove7.common.utils.runOnNewHandlerThread
import cn.vove7.jarvis.tools.AppConfig


/**
 * # AssistSession
 * 会话界面
 */
@RequiresApi(api = Build.VERSION_CODES.M)
class AssistSession(context: Context) : VoiceInteractionSession(context),
        SimpleListAdapter.OnItemClickListener {
    lateinit var bottomSheetController: BottomSheetController
    private var pb: ProgressBar? = null
    private var screenshot: Bitmap? = null
    var screenPath: String? = null
    override fun onAssistStructureFailure(failure: Throwable) {
        failure.printStackTrace()
        Vog.d(this, "onAssistStructureFailure ---> ${failure.message}")
    }

    override fun onCreate() {
        super.onCreate()
        AppBus.reg(this)
    }

    override fun onHandleScreenshot(screenshot: Bitmap?) {
        if (screenshot == null) return
        screenPath = "loading"
        showProgressBar = true
        runOnNewHandlerThread("save_screen") {
            this@AssistSession.screenshot = screenshot
            val ss = compressMaterix(screenshot)
            Vog.d(this, "onHandleScreenshot ---> $screenshot")
            screenPath = UtilBridge.bitmap2File(ss, context.cacheDir
                    .absolutePath + "/screen.png")?.absolutePath
            if (!UserInfo.isVip())
                sleep(500)
            showProgressBar = false
        }
    }

    override fun onHandleAssist(data: Bundle?, structure: AssistStructure?, content: AssistContent?) {
        data?.keySet()?.forEach {
            Vog.d(this, "onHandleAssist ---> ${data.get(it)}")
        }
    }

    override fun onCreateContentView(): View {
        val view = layoutInflater.inflate(R.layout.dialog_assist, null)
        pb = view.findViewById(R.id.progress_bar)
        view.findViewById<View>(R.id.voice_btn).setOnClickListener {
            MainService.switchReco()
            onBackPressed()
        }
        showProgressBar = showProgressBar
        bottomSheetController = BottomSheetController(context, view.findViewById(R.id.bottom_sheet))
        bottomSheetController.setBottomListData(items, this)
        bottomSheetController.bottomView.post {
            bottomSheetController.showBottom()
        }
        bottomSheetController.behavior?.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(p0: View, p1: Int) {
                if (p1 == BottomSheetBehavior.STATE_HIDDEN) finish()
            }

            override fun onSlide(p0: View, p1: Float) {}
        })
        view.findViewById<View>(R.id.root).setOnClickListener { onBackPressed() }
        return view
    }

    private var showProgressBar: Boolean = false
        set(value) {
            runOnUi {
                if (value) pb?.visibility = View.VISIBLE
                else pb?.visibility = View.INVISIBLE
            }
            field = value
        }

    override fun onClick(holder: SimpleListAdapter.VHolder?, pos: Int, item: ViewModel) {
        when (pos) {
            0 -> {
                val path = screenPath
                when (path) {
                    null -> GlobalApp.toastShort("屏幕内容获取失败")
                    "loading" -> GlobalApp.toastShort("等待加载完成")
                    else -> imageClassify(path)
                }
            }
            1 -> {//todo
                GlobalApp.toastShort(R.string.text_coming_soon)
//                if (!AppConfig.checkUser()) return


            }
            2 -> {
                AppBus.postDelay("0_0", ORDER_BEGIN_SCREEN_PICKER, 800)
                onBackPressed()
            }
            3 -> {
                SystemBridge.shareImage(screenPath)
                onBackPressed()
            }
            4 -> {
                when (screenPath) {
                    null -> GlobalApp.toastShort("屏幕内容获取失败")
                    "loading" -> GlobalApp.toastShort("等待加载完成")
                    else -> {
                        showProgressBar = true
                        QRTools.parseBitmap(screenshot!!) {
                            runOnUi {
                                showProgressBar = false
                                onScanQRCodeSuccess(it)
                            }
                        }
                    }
                }
            }
            5 -> {
                val ss = screenshot
                when (ss) {
                    null -> GlobalApp.toastShort("屏幕内容获取失败")
                    else -> {
                        showProgressBar = true
                        runOnNewHandlerThread {
                            UtilBridge.bitmap2File(ss, Environment.getExternalStorageDirectory()
                                    .absolutePath + "/Pictures/Screenshots/Screenshot_${formatNow("yyyyMMdd-HHmmss")}.jpg").also {
                                if (it != null) GlobalApp.toastLong("保存到 ${it.absolutePath}")
                            }
                            showProgressBar = false
                        }
                    }
                }
            }
        }
    }

    private val items = mutableListOf(
            ViewModel("屏幕识别", icon = context.getDrawable(R.drawable.ic_screen_content), subTitle = "识别屏幕内容"),
            ViewModel("文字识别", icon = context.getDrawable(R.drawable.ic_screen_text), subTitle = "适用于图片中的文字识别"),
            ViewModel("文字提取", icon = context.getDrawable(R.drawable.ic_tt), subTitle = "适用于屏幕内文本提取"),
            ViewModel("分享屏幕", icon = context.getDrawable(R.drawable.ic_screenshot), subTitle = "分享截屏"),
            ViewModel("二维码/条码识别", icon = context.getDrawable(R.drawable.ic_qr_code)),
            ViewModel("保存截图", icon = context.getDrawable(R.drawable.ic_screenshot))
    )

    /**
     * 带有动画，退出
     */
    override fun onBackPressed() {//ani
        bottomSheetController.hideBottom()
    }

    override fun onHide() {
        Vog.d(this, "onHide ---> ")
        AppBus.unreg(this)
//        AppBus.post(AppBus.ORDER_CANCEL_RECO)
        dialog?.dismiss()
        finish()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(e: String) {
        when (e) {
            EVENT_HIDE_FLOAT -> onBackPressed()
            EVENT_BEGIN_RECO -> {//开始识别

            }
            EVENT_FINISH_RECO -> {

            }
            EVENT_ERROR_RECO -> {

            }
            else -> {
            }
        }
    }

    private fun onScanQRCodeSuccess(result: String?) {
        Vog.d(this, "onScanQRCodeSuccess ---> $result")
        if (result == null) {
            GlobalApp.toastShort("无识别结果")
            return
        }

        AlertDialog.Builder(context).setTitle("识别结果")
                .setMessage(result)
                .setPositiveButton("复制") { _, _ -> SystemBridge.setClipText(result) }
                .setNegativeButton("分享") { _, _ -> SystemBridge.shareText(result) }
                .also {
                    when {
                        result.startsWith("http", ignoreCase = true) -> {
                            it.setNeutralButton("访问") { _, _ ->
                                hide()
                                SystemBridge.openUrl(result)
                            }
                        }
                        TextHelper.isEmail(result) -> {
                            it.setNeutralButton("发邮件") { _, _ ->
                                hide()
                                SystemBridge.sendEmail(result)
                            }
                        }
                        result.startsWith("market:", ignoreCase = true) -> {
                            it.setNeutralButton("打开应用市场") { _, _ ->
                                hide()
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.data = Uri.parse(result)
                                //跳转酷市场
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            }
                        }
                        result.startsWith("smsto:", ignoreCase = true) -> {
                            it.setNeutralButton("发送短信") { _, _ ->
                                val ss = result.split(':')
                                val p = try {
                                    ss[1]
                                } catch (e: Exception) {
                                    GlobalApp.toastShort("无手机号")
                                    return@setNeutralButton
                                }
                                hide()
                                val content = try {
                                    ss[2]
                                } catch (e: Exception) {
                                    ""
                                }
                                SystemBridge.sendSMS(p, content)
                            }
                        }
                        result.startsWith("tel:", ignoreCase = true) -> {
                            it.setNeutralButton("拨号") { _, _ ->
                                hide()
                                SystemBridge.call(result.substring(4))
                            }
                        }
                    }
                    it.create().apply {
                        try {
                            DialogUtil.setFloat(this)//悬浮权限
                            show()
                        } catch (e: Exception) {
                            AppBus.post(RequestPermission("悬浮窗权限"))
                        }
                    }
                }

//        MaterialDialog(context).title(text = "识别结果")
//                .message(text = result)
//                .positiveButton(text = "复制") { SystemBridge.setClipText(result) }
//                .negativeButton(text = "分享") { SystemBridge.shareText(result) }
//                .show()
    }

    var dialog: Dialog? = null
    /**
     * 图像识别
     * @param path String
     */
    private fun imageClassify(path: String) {
        showProgressBar = true
        thread {
            val r = BaiduAipHelper.imageClassify(path)
            runOnUi {
                showProgressBar = false
                Vog.d(this, "imageClassify ---> ${r?.bestResult}")
                val result = r?.bestResult
                if (r?.hasErr == false && result != null) {
                    if (result.keyword == "屏幕截图") {
                        GlobalApp.toastShort("无识别结果")
                    } else {
                        dialog = ImageClassifyResultDialog(result, context, screenshot) {
                            onBackPressed()
                        }.also { it.show() }
                    }
                } else {
                    GlobalApp.toastShort("识别失败")
                }
            }
        }
    }

    fun compressMaterix(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.setScale(0.5f, 0.5f)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}

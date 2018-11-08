package cn.vove7.common.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat.FOCUS_INPUT
import cn.vove7.common.accessibility.viewnode.ViewNode
import cn.vove7.common.app.GlobalLog
import cn.vove7.common.datamanager.parse.model.ActionScope
import cn.vove7.common.executor.CExecutorI
import cn.vove7.common.view.finder.ViewFinder
import cn.vove7.vtp.app.AppInfo

/**
 *
 *
 * Created by Vove on 2018/6/18
 */
abstract class AccessibilityApi : AccessibilityService(),
        AccessibilityBridge {
    abstract fun getService(): AccessibilityService

    val currentScope = ActionScope()
    var currentActivity: String? = null
        private set
        get() = currentScope.activity

    override fun onCreate() {
        super.onCreate()
        accessibilityService = this
    }

    val currentFocusedEditor: ViewNode?
        get() = findFocus(FOCUS_INPUT).let {
            if (it == null) null else ViewNode(it)
        }

    /**
     * 禁用软键盘，并且无法手动弹出
     * @return Boolean
     */
    fun disableSoftKeyboard(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            softKeyboardController.setShowMode(SHOW_MODE_HIDDEN)
        } else {
            GlobalLog.log("7.0以下不支持hideSoftKeyboard")
            false
        }
    }

    fun enableSoftKeyboard(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            softKeyboardController.setShowMode(SHOW_MODE_AUTO)
        } else {
            GlobalLog.log("7.0以下不支持hideSoftKeyboard")
            false
        }
    }

    /**
     * 省电模式
     */
    abstract fun powerSavingMode()

    /**
     * 关闭省电
     */
    abstract fun disablePowerSavingMode()

    var currentAppInfo: AppInfo? = null
        protected set

    companion object {
        var accessibilityService: AccessibilityApi? = null
        fun isOpen(): Boolean {
            return accessibilityService != null
        }
    }

}

interface AccessibilityBridge {
    /**
     * 等待出现指定View  with /id/text/desc
     * 特殊标记
     */
    fun waitForView(executor: CExecutorI, finder: ViewFinder)

    fun getRootViewNode(): ViewNode?
    fun waitForActivity(executor: CExecutorI, scope: ActionScope)
    /**
     * remove all notifier when was interrupted
     */
    fun removeAllNotifier(executor: CExecutorI)
}



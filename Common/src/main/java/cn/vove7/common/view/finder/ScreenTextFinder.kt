package cn.vove7.common.view.finder

import android.view.accessibility.AccessibilityNodeInfo
import cn.vove7.common.accessibility.AccessibilityApi

/**
 * # ScreenTextFinder
 *
 * @author Administrator
 * 2018/10/14
 */
class ScreenTextFinder(accessibilityApi: AccessibilityApi) : ViewFinder(accessibilityApi) {

    override fun findCondition(node: AccessibilityNodeInfo): Boolean {
        return node.text != null
    }

}
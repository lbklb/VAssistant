package cn.vove7.common.viewnode

import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import cn.vove7.common.app.GlobalApp
import cn.vove7.common.app.GlobalLog
import cn.vove7.vtp.log.Vog
import cn.vove7.vtp.text.TextTransHelper
import java.lang.Thread.sleep

/**
 * 视图节点
 */
class ViewNode(val node: AccessibilityNodeInfo) : ViewOperation, Comparable<ViewNode> {

    /**
     * 文本相似度
     */
    var similarityText: Float = 0f

    companion object {
        const val tryNum = 10
    }

    fun getBoundsInParent(): Rect {
        val out = Rect()
        node.getBoundsInParent(out)
        return out
    }

    fun getBounds(): Rect {
        val out = Rect()
        node.getBoundsInScreen(out)
        return out
    }

    fun getParent(): ViewNode? {
        val it = node.parent
        return if (it != null) {
            ViewNode(it)
        } else
            null
    }

    override fun tryClick(): Boolean = tryOp(AccessibilityNodeInfo.ACTION_CLICK)

    /**
     * 尝试操作次数
     * 点击，长按，选择
     * 尝试点击父级
     */
    private fun tryOp(action: Int): Boolean {
        var p = node
        var i = 0
        while (i < tryNum && !p.performAction(action)) {
            if (p.parent == null) {
                Vog.d(this, "尝试->$i p.parent == null")
                return false
            }
            p = p.parent
            i++
        }
        val b = i != tryNum
        Vog.d(this, "尝试->$i $b")
        return b
    }

    /**
     * @return node.childs
     */
    fun childs(): List<ViewNode> {
        val cs = mutableListOf<ViewNode>()
        for (i in 0 until node.childCount) {
            val c = node.getChild(i)
            if (c != null) {
                cs.add(ViewNode(c))
            }
        }
        return cs
    }

    override fun click(): Boolean = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)

    override fun doubleClick(): Boolean {
        return if (tryClick()) {
            sleep((ViewConfiguration.getDoubleTapTimeout() + 50).toLong())
            tryClick()
        } else false
    }

    override fun tryLongClick(): Boolean {
        return tryOp(AccessibilityNodeInfo.ACTION_LONG_CLICK)
    }

    override fun longClick(): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
    }

    override fun select(): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_SELECT)
    }

    override fun trySelect(): Boolean {
        return tryOp(AccessibilityNodeInfo.ACTION_SELECT)
    }

    override fun scrollUp(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP.id)
        } else {
            GlobalLog.err("scrollUp need SDK M")
            false
        }
    }

    override fun scrollDown(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN.id)
        } else {
            GlobalLog.err("scrollDown need SDK M")
            false
        }
    }

    override fun scrollForward(): Boolean {
        return node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD.id)
    }

    override fun scrollBackward(): Boolean {
        return node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD.id)
    }

    override fun scrollLeft(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_LEFT.id)
        } else {
            GlobalLog.err("scrollLeft need SDK M")
            false
        }
    }

    override fun scrollRight(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT.id)
        } else {
            GlobalLog.err("scrollRight need SDK M")
            false
        }
    }

    override fun getText(): String? {
        val text = node.text
        Vog.d(this, "$text")
        return text as String?
    }

    /**
     * @param ep 额外参数
     */
    override fun setText(text: String, ep: String?): Boolean {
        val arg = Bundle()
        arg.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, transText(text, ep))
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arg)
    }

    override fun setTextWithInitial(text: String): Boolean {
        return setText(text, "1")
    }

    override fun setText(text: String): Boolean {
        val arg = Bundle()
        arg.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arg)
    }

    /**
     * text转变
     */
    private fun transText(text: String, ep: String?): String {
        if (ep == null) return text
        return when (ep) {
            "1" -> {//转中文拼音首字母
                TextTransHelper(GlobalApp.APP).chineseStr2Pinyin(text, true)
            }
            else -> {
                text
            }
        }.also {
            Vog.d(this, "transText $ep $it")
        }
    }

    override fun trySetText(text: String): Boolean {
        val arg = Bundle()
        arg.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        var p = node
        var i = 0
        while (i < tryNum && !p.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arg)) {
            p = node.parent
            i++
        }
        val b = i != tryNum
        Vog.d(this, "尝试-> $b")
        return b
    }

    override fun focus(): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
    }

    override fun compareTo(other: ViewNode): Int {
        return ((other.similarityText - similarityText) * 100).toInt()
    }

    override fun toString(): String {
        return nodeSummary(node)
    }

    private fun nodeSummary(node: AccessibilityNodeInfo?): String {
        if (node == null) return "null\n"
        val clsName = node.className
        val id = node.viewIdResourceName
        val rect = Rect()
        node.getBoundsInScreen(rect)
        val cls = clsName.substring(clsName.lastIndexOf('.') + 1)
        return String.format("[cls: %s] [id: %s] [desc: %s] [text: %s] [%s]",
                cls, id?.substring(id.lastIndexOf('/') + 1) ?: "null",
                node.contentDescription, node.text, getBounds()
        )
    }

}

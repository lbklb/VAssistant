package cn.vove7.common.view.finder

import android.view.accessibility.AccessibilityNodeInfo
import cn.vove7.common.accessibility.AccessibilityApi
import cn.vove7.common.accessibility.viewnode.ViewNode
import cn.vove7.vtp.log.Vog

/**
 * 查找符合条件的AccessibilityNodeInfo
 */
abstract class ViewFinder(var accessibilityService: AccessibilityApi) {

    open fun findFirst(): ViewNode? {
        val r = traverseAllNode(accessibilityService.rootInActiveWindow)
        Vog.i(this, "findFirst ${r != null}")
        return r
    }

    val list = mutableListOf<ViewNode>()

    fun findAll(): Array<ViewNode> {
        list.clear()
        traverseAllNode(accessibilityService.rootInActiveWindow, true)
        val l = mutableListOf<ViewNode>()
        l.addAll(list)
        return l.toTypedArray()
    }

    /**
     * 深搜遍历
     *
     * @param node AccessibilityNodeInfo?
     * @param all Boolean true 搜索全部返回list else return first
     * @return ViewNode?
     */
    private fun traverseAllNode(node: AccessibilityNodeInfo?, all: Boolean = false): ViewNode? {
        if (node == null) return null
        (0 until node.childCount).forEach { index ->
            Vog.v(this, "traverseAllNode ${node.className} $index/${node.childCount}")
            val childNode = node.getChild(index)
            if (childNode != null) {
                if (!childNode.isVisibleToUser) {//TODO check it
                    return@forEach
                }
                if (findCondition(childNode)) {
                    if (all) {
                        list.add(ViewNode(childNode))
                    } else return ViewNode(childNode)
                } else {
                    if (all) {
                        traverseAllNode(childNode, true)
                    } else {
                        val r = traverseAllNode(childNode)
                        if (r != null) return r
                    }
                    //深搜
                }
            }
        }
        return null
    }

    /**
     * 查找条件
     */
    abstract fun findCondition(node: AccessibilityNodeInfo): Boolean

}
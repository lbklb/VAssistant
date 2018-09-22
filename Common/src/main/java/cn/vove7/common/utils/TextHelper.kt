package cn.vove7.common.utils

/**
 * # TextHelper
 *
 * @author 17719247306
 * 2018/9/12
 */
object TextHelper {

    private val emailValid = Regex("[^@]([\\S])*?@([\\S])*?\\.([\\S])+")

    fun isEmail(s: String?): Boolean {
        return s != null && emailValid.matches(s)
    }

    private val userRegex = Regex("[0-9a-zA-Z[\\u4e00-\\u9fa5]]+")
    fun checkUserName(s: String?): Boolean {
        if (s == null) return false
        return userRegex.matches(s)
    }

    fun arr2String(ss: Array<*>, separator: String = ","): String {
        return buildString {
            ss.withIndex().forEach {
                if (it.index == 0) append(ss[0])
                else
                    append(separator + ss[it.index])
            }
        }
    }

    /**
     * 包含比例
     * 长度小于3 不比较
     */
    fun containsRatio(s1: String, s2: String): Float {
        val shorter: String
        val lener = if (s1.length > s2.length) {
            shorter = s2
            s1
        } else {
            shorter = s1
            s2
        }
        if (lener.length <= 3) return 0f
        return if (lener.contains(shorter, ignoreCase = true)) {
            shorter.length.toFloat() / lener.length
        } else 0f
    }
}
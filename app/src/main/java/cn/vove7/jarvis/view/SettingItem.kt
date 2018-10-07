package cn.vove7.jarvis.view

import android.support.annotation.ArrayRes
import cn.vove7.common.app.GlobalApp
import cn.vove7.jarvis.utils.AppConfig
import cn.vove7.jarvis.view.utils.SettingItemHelper

/**
 * # SettingItem
 *
 * @author 17719247306
 * 2018/9/10
 */

const val TYPE_CHECK_BOX = 1
const val TYPE_SWITCH = 2
const val TYPE_INPUT = 3
const val TYPE_SINGLE = 4
@Deprecated("unused")
const val TYPE_MULTI = 5
const val TYPE_NUMBER = 6
const val TYPE_SWITCH_CALLBACK = 8
const val TYPE_INTENT = 9

typealias CallbackOnSet = (SettingItemHelper.ChildItemHolder, Any) -> Unit

/**
 * 基类
 *
 * 标题 summary 其他为,,,
 * @property titleId Int
 * @property summary String?
 * @property itemType Int
 * @property keyId Int?
 * @property defaultValue Function0<Any>
 * @property range Pair<Int, Int>?
 * @property callback Function1<Any, Unit>?
 * @property entityArrId Int?
 * @constructor
 */
open class SettingChildItem(
        val titleId: Int?,
        val title: String?,
        var summary: String? = null,
        val itemType: Int,
        val keyId: Int? = null,
        val defaultValue: () -> Any,
//        val autoSetValue: Boolean = keyId!=null,
        val range: Pair<Int, Int>? = null,
        val callback: CallbackOnSet? = reloadConfig,
        val entityArrId: Int? = null,
//        val valueArrId: Int? = null,
        val items: List<String>? = null
) {
    fun title(): String {
        if (titleId != null)
            return GlobalApp.getString(titleId)
        if (title != null)
            return title

        return ".."
    }
}

val reloadConfig: CallbackOnSet = { _, _ ->
    AppConfig.reload()
}

class CheckBoxItem(
        titleId: Int? = null,
        title: String? = null,
        summary: String? = null,
        keyId: Int? = null,
        defaultValue: () -> Boolean,
        callback: CallbackOnSet? = reloadConfig
) : SettingChildItem(titleId, title, summary, TYPE_CHECK_BOX, keyId, defaultValue,
        callback = callback)

class SwitchItem(
        titleId: Int? = null,
        title: String? = null,
        summary: String? = null,
        keyId: Int? = null,
        defaultValue: () -> Boolean,
        callback: CallbackOnSet? = reloadConfig
) : SettingChildItem(titleId, title, summary, TYPE_SWITCH, keyId, defaultValue, callback = callback)


class NumberPickerItem(
        titleId: Int? = null,
        title: String? = null,
        summary: String? = null,
        keyId: Int? = null,
        defaultValue: () -> Int,
        range: Pair<Int, Int>,
        callback: CallbackOnSet? = reloadConfig
) : SettingChildItem(titleId, title, summary, TYPE_NUMBER, keyId, defaultValue, range = range,
        callback = callback)

class SingleChoiceItem(
        titleId: Int? = null,
        title: String? = null,
        summary: String? = null,
        keyId: Int? = null,
        defaultValue: () -> Int,//pos
        @ArrayRes entityArrId: Int? = null,
        callback: CallbackOnSet? = reloadConfig,
        items: List<String>? = null
) : SettingChildItem(titleId, title, summary, TYPE_SINGLE, keyId, defaultValue,
        entityArrId = entityArrId,  callback = callback, items = items)

class IntentItem(titleId: Int? = null,
                 title: String? = null,
                 summary: String? = null,
                 onClick: CallbackOnSet)
    : SettingChildItem(titleId, title, summary, itemType = TYPE_INTENT,
        defaultValue = { Any() }, callback = onClick)

class InputItem(titleId: Int? = null,
                title: String? = null,
                summary: String? = null,
                defaultValue: () -> String,
                callback: CallbackOnSet)
    : SettingChildItem(titleId, title, summary, itemType = TYPE_INPUT,
        defaultValue = defaultValue, callback = callback)
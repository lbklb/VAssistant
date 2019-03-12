package cn.vove7.jarvis.view.dialog

import android.content.Context
import cn.vove7.executorengine.bridges.SystemBridge
import cn.vove7.jarvis.R
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import kotlinx.android.synthetic.main.edit_view.view.*

/**
 * # TextEditorDialog
 *
 * @author 11324
 * 2019/3/12
 */
class TextEditorDialog(context: Context, val text: String) {
    init {
        MaterialDialog(context).show {
            title(text = "编辑")
            val v = layoutInflater.inflate(R.layout.edit_view, null)
            customView(view = v, scrollable = true)
            v.editText.setText(text)
            positiveButton(text = "复制") {
                SystemBridge.setClipText(v.editText.text.toString())
            }
            negativeButton(text = "分享") {
                SystemBridge.shareText(v.editText.text.toString())
            }
        }
    }

}
package cn.vove7.jarvis.view.dialog

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.SpannableStringBuilder
import android.view.Gravity
import android.widget.TextView
import cn.vove7.common.view.editor.MultiSpan
import cn.vove7.jarvis.R
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView

/**
 * # ProgressTextDialog
 *
 * @author Administrator
 * 2018/10/18
 */
class ProgressTextDialog(val context: Context, val title: String? = null,
                         val cancelable: Boolean = true) {
    val dialog = MaterialDialog(context)
    val textView = TextView(context)

    val handler = UiHandler(textView, Looper.getMainLooper())

    init {
        textView.setPadding(60, 0, 60, 0)
        textView.gravity = Gravity.BOTTOM
        textView.setTextColor(context.resources.getColor(R.color.primary_text))
        dialog.title(text = title)
                .customView(view = textView, scrollable = true)
                .cancelable(cancelable)
                .show()
        dialog
    }

    @Synchronized
    fun appendln(s: Any) {
        append(s)
        append("\n")
    }

    fun appendlnGreen(s: String) {
        appendlnColor(s,R.color.green_700)
    }

    fun appendlnRed(s: String) {
        appendlnColor(s,R.color.red_900)
    }
    fun appendlnAmber(s: String) {
        appendlnColor(s,R.color.amber_A700)
    }

    private fun appendlnColor(s: String, color:Int) {
        val ss = MultiSpan(context, s, color).spanStr
        appendln(ss)
    }

    fun append(s: Any) {
        handler.sendMessage(handler.obtainMessage(0, s))
    }

    fun finish() {
        Handler(Looper.getMainLooper()).post {
            dialog.positiveButton { it.dismiss() }
        }
    }

    class UiHandler(val textView: TextView, loop: Looper) : Handler(loop) {
        override fun handleMessage(msg: Message?) {

            val it = msg?.obj
            when (it) {
                is CharSequence -> {
                    textView.append(it)
                }
                is SpannableStringBuilder -> {
                    textView.append(it)
                }
            }

        }
    }


}
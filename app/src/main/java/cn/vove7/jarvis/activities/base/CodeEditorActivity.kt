package cn.vove7.jarvis.activities.base

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.SpannableStringBuilder
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import cn.vove7.androlua.luabridge.LuaUtil
import cn.vove7.common.app.GlobalLog
import cn.vove7.common.appbus.AppBus
import cn.vove7.common.datamanager.parse.model.Action
import cn.vove7.common.datamanager.parse.model.ActionParam
import cn.vove7.common.executor.OnPrint
import cn.vove7.common.interfaces.CodeEditorOperation
import cn.vove7.common.view.editor.MultiSpan
import cn.vove7.common.view.toast.ColorfulToast
import cn.vove7.jarvis.R
import cn.vove7.jarvis.utils.UriUtils
import cn.vove7.jarvis.view.EditorFunsHelper
import cn.vove7.vtp.log.Vog
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.input.input
import kotlinx.android.synthetic.main.editor_tool_bar.*


/**
 * # CodeEditorActivity
 *
 * @author Administrator
 * 2018/10/9
 */
abstract class CodeEditorActivity : AppCompatActivity() {

    abstract val codeEditor: CodeEditorOperation

    var openFile: String? = null
    val toast: ColorfulToast by lazy {
        ColorfulToast(this).bottom()
    }
    abstract val testFiles: Array<String>
    abstract val assetFolder: String

    abstract val scriptType: String

    inner class MyPrinter : OnPrint {
        override fun onPrint(l: Int, output: String) {
            val pair = when (l) {
                OnPrint.ERROR -> Pair(cn.vove7.vtp.R.color.red_500, "ERROR: ")
                OnPrint.WARN -> Pair(cn.vove7.vtp.R.color.orange_700, "WARN: ")
                OnPrint.INFO -> Pair(cn.vove7.vtp.R.color.orange_700, "INFO: ")
                else -> Pair(cn.vove7.vtp.R.color.default_text_color, "")
            }

            val i = MultiSpan(this@CodeEditorActivity, pair.second, colorId = pair.first)
            val v = MultiSpan(this@CodeEditorActivity, output, colorId = pair.first)
            synchronized(logList) {
                logList.add(i.spanStr)
                logList.add(v.spanStr)
            }
            Handler(Looper.getMainLooper()).post {
                logView?.append(i.spanStr)
                logView?.append(v.spanStr)
            }
        }
    }

    var print: OnPrint = MyPrinter()

    var runArgs: String? = null

    var loaded = false
    override fun onStart() {
        super.onStart()
        if (!loaded) {
            if (testFiles.isNotEmpty()) {
                setCode(LuaUtil.getTextFromAsset(this, assetFolder + testFiles[0]))
            }
            initView()
            initEditorToolbar()
            loaded = true
        }
    }

    private fun hideInputMethod() {
        val mInputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        mInputManager.hideSoftInputFromWindow(findViewById<View>(R.id.root).windowToken, 0)
    }

    private fun initView() {
        toggle_functions.setOnClickListener {
            if (functions_grid.visibility == View.GONE) {
                functions_grid.visibility = View.VISIBLE
                activityRootView.viewTreeObserver.removeOnGlobalLayoutListener(gloLis)
                hideInputMethod()
                Handler().postDelayed({
                    activityRootView.viewTreeObserver.addOnGlobalLayoutListener(gloLis)
                }, 100)
            } else functions_grid.visibility = View.GONE
            functions_grid.animate().setDuration(1000).start()
        }
        EditorFunsHelper(this, supportFragmentManager, func_pager, tab_lay) {
            codeEditor.insert(it)
        }
        activityRootView.viewTreeObserver.addOnGlobalLayoutListener(gloLis)
    }

    private val gloLis = ViewTreeObserver.OnGlobalLayoutListener {
        val heightDiff = activityRootView.rootView.height - activityRootView.height
        Vog.d(this, "initView ---> $heightDiff")
        if (heightDiff > 500) {
            functions_grid.visibility = View.GONE
        }
    }
    private val activityRootView: View by lazy { findViewById<View>(R.id.root) }

    private fun initEditorToolbar() {
        symbol_line.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        symbol_line.adapter = SymbolsAdapter(this, codeEditor, symbols)
    }

    abstract val symbols: List<Symbol>

    private val haveFileUnsaved: Boolean
        get() = openFile != null && codeEditor.isEdit

    override fun onBackPressed() {
        if (haveFileUnsaved) {
            AlertDialog.Builder(this).setTitle("提示")
                    .setMessage("有未保存的文件，是否放弃保存")
                    .setPositiveButton(R.string.text_ok) { _, _ ->
                        finish()
                    }.setNegativeButton(R.string.text_cancel) { _, _ ->

                    }.setNegativeButton("保存退出") { _, _ ->
                        codeEditor.saveFile(openFile!!)
                        finish()
                    }
                    .show()
            return
        } else super.onBackPressed()
    }


    private val logList = mutableListOf<SpannableStringBuilder>()

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_code_run -> {
                clearLog()
                val ac = Action(editorText, scriptType)
                ac.param = ActionParam()
                ac.param.value = runArgs
                AppBus.post(ac)
            }
            R.id.menu_set_arg -> {
                MaterialDialog(this)
                        .title(text = "设置参数")
                        .input(prefill = runArgs) { materialDialog, charSequence ->
                            runArgs = charSequence.toString()
                            toast.showShort("参数已设置")
                        }.positiveButton()
                        .show()
            }

            R.id.menu_code_stop -> {
                AppBus.post(AppBus.ORDER_STOP_EXEC)
            }
            R.id.menu_sel_code -> {
                AlertDialog.Builder(this).setTitle(R.string.text_select_script).setItems(testFiles) { d, p ->
                    openFile = null
                    setCode(LuaUtil.getTextFromAsset(this@CodeEditorActivity,
                            assetFolder + testFiles[p]))
                    d.dismiss()
                }.show()
            }
            R.id.menu_goto_line -> {
                codeEditor.gotoLine()
            }
            R.id.menu_undo -> {
                codeEditor.undo()
            }
            R.id.menu_redo -> codeEditor.redo()
            R.id.menu_find -> {
                codeEditor.find()
            }
            R.id.menu_switch_ui_mode -> {
//                dark = !dark
//                codeEditor.setDark(dark)
            }
            R.id.menu_code_format -> codeEditor.format()
            R.id.menu_save_file -> {
                if (openFile != null) {
                    codeEditor.saveFile(openFile!!)
                } else {
                    toast.showShort("示例文件无法保存")
                }
            }
            R.id.menu_log -> showLog()
            R.id.menu_open_file -> {
                if (haveFileUnsaved) {
                    AlertDialog.Builder(this).setTitle("提示")
                            .setMessage("有未保存的文件，是否放弃保存")
                            .setPositiveButton(R.string.text_ok) { _, _ ->
                                openFile()
                            }.setNegativeButton(R.string.text_cancel) { _, _ ->
                            }.setNegativeButton("保存") { _, _ ->
                                codeEditor.saveFile(openFile!!)
                                openFile()
                            }
                            .show()
                    return true
                } else openFile()
            }
            else -> {
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun showLog() {
        MaterialDialog(this)
                .title(text = "输出")
                .customView(view = buildLogText(), scrollable = true)
                .neutralButton(text = "清空") {
                    clearLog()
                    showLog()
                }
                .positiveButton()
                .onDismiss {
                    logView = null
                }
                .show()
    }

    var logView: TextView? = null
    private fun buildLogText(): TextView {
        logView = TextView(this)
        logView?.setPadding(50, 0, 50, 0)
        logView?.gravity = Gravity.BOTTOM
        synchronized(logList) {
            listOf(*logList.toTypedArray()).forEach {
                logView?.append(it)
            }
        }
        return logView!!
    }

    private fun openFile() {
        val selIntent = Intent(Intent.ACTION_GET_CONTENT)
        selIntent.type = "*/*"
        selIntent.addCategory(Intent.CATEGORY_OPENABLE)
        try {
            startActivityForResult(selIntent, 1)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
            toast.showShort(getString(R.string.text_cannot_open_file_manager))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {//选择文件回调
            when (requestCode) {
                1 -> {
                    val uri = data?.data
                    if (uri != null) {
                        try {
                            val path = UriUtils.getPathFromUri(this, uri)
                            codeEditor.openFile(path!!)
                            codeEditor.isEdit = false
                            openFile = path
                        } catch (e: Exception) {
                            GlobalLog.err(e)
                            toast.showShort(getString(R.string.text_open_failed))
                        }
                    } else {
                        toast.showShort(getString(R.string.text_open_failed))
                    }
                }
                else -> {
                }
            }
        }
    }

//    var dark: Boolean = false
//        get() = SpHelper(this).getBoolean("editor_dark", false)
//        set(value) {
//            SpHelper(this).set("editor_dark", value)
//            field = value
//        }

    private fun clearLog() {
        logList.clear()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_code_editor, menu)
        return true
    }

    private fun setCode(s: String) {
        codeEditor.setText(s)
    }

    private val editorText: String
        get() = codeEditor.getEditorContent() ?: ""

    class SymbolsAdapter(val c: Context, val editor: CodeEditorOperation, val symbols: List<Symbol>) : RecyclerView.Adapter<V>() {
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): V {
            val v = LayoutInflater.from(c).inflate(R.layout.item_of_symbols, null)
            return V(v)
        }

        override fun getItemCount(): Int = symbols.size


        override fun onBindViewHolder(p0: V, p1: Int) {
            (p0.textView as TextView).apply {
                text = symbols[p1].show
                setOnClickListener {
                    editor.insert(symbols[p1].fillText)
                }
            }
        }
    }

    class V(v: View) : RecyclerView.ViewHolder(v) {
        val textView = v
    }

    companion object {
        val commonSymbols = listOf(Symbol("("),
                Symbol(")"), Symbol("<"),
                Symbol(">"), Symbol("{"),
                Symbol("}"), Symbol("["), Symbol("]"),
                Symbol("\""), Symbol("'"), Symbol("/"),
                Symbol("|"), Symbol("+"),
                Symbol("-"), Symbol("\\"), Symbol("*"), Symbol("?"),
                Symbol("&"), Symbol("="), Symbol("%"), Symbol("")
        )
        val jsSymbols = mutableListOf(
                Symbol("⇥", "  "),
                Symbol("fun", "function () {\n  \n}\n"),
                Symbol("!")
        ).also { it.addAll(commonSymbols) }
        val luaSymbols = mutableListOf(
                Symbol("⇥", "  "),
                Symbol("fun", "function ()\n  \nend\n"),
                Symbol("not"), Symbol("end"), Symbol("then"),
                Symbol("#")
        ).also { it.addAll(commonSymbols) }
    }

    class Symbol(
            val show: String,
            val fillText: String = show
    )
}
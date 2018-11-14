package cn.vove7.jarvis.tools

import android.app.Activity
import android.graphics.Color
import cn.vove7.common.app.GlobalApp
import cn.vove7.common.app.GlobalLog
import cn.vove7.common.datamanager.AppAdInfo
import cn.vove7.common.datamanager.DAO
import cn.vove7.common.datamanager.DaoHelper
import cn.vove7.common.datamanager.OnUpdate
import cn.vove7.common.datamanager.executor.entity.MarkedData
import cn.vove7.common.datamanager.parse.statusmap.ActionNode
import cn.vove7.common.model.ResultBox
import cn.vove7.common.netacc.ApiUrls
import cn.vove7.common.netacc.NetHelper
import cn.vove7.common.netacc.model.BaseRequestModel
import cn.vove7.common.netacc.model.LastDateInfo
import cn.vove7.common.netacc.model.ResponseMessage
import cn.vove7.common.utils.TextHelper
import cn.vove7.common.utils.ThreadPool.runOnPool
import cn.vove7.executorengine.helper.AdvanAppHelper
import cn.vove7.executorengine.parse.ParseEngine
import cn.vove7.jarvis.R
import cn.vove7.jarvis.plugins.AdKillerService
import cn.vove7.jarvis.view.dialog.ProgressTextDialog
import cn.vove7.vtp.log.Vog
import cn.vove7.vtp.sharedpreference.SpHelper
import com.afollestad.materialdialogs.MaterialDialog
import com.google.gson.reflect.TypeToken
import kotlin.concurrent.thread

/**
 * # DataUpdator
 *
 * @author Administrator
 * 2018/10/15
 */
object DataUpdator {

    /**
     * 启动时检查更新
     * @param activity Activity
     */
    fun checkUpdate(activity: Activity, back: () -> Unit) {
        NetHelper.getLastInfo {
            if (it != null) {
                val v = check(it)
                if (v.first.isNotEmpty()) {
                    notifyUpdate(activity, v.first, v.second, back)
                } else {
                    back.invoke()
                }
            } else {
                Vog.d(this, "checkDataUpdate ---> 检查数据失败")
                back.invoke()
            }
        }
    }

    /**
     * 弹出更新对话框，更新内容
     * @param activity Activity
     * @param needUpdateTypes List<Int>
     * @param s String
     */
    private fun notifyUpdate(activity: Activity, needUpdateTypes: List<Int>, s: String, back: () -> Unit) {
        val d = MaterialDialog(activity).title(text = "数据更新")
                .message(text = "检查到以下数据有更新\n$s")
                .positiveButton(text = "立即同步") {
                    oneKeyUpdate(activity, needUpdateTypes, back)
                }
                .cancelable(false)
                .negativeButton()
        if (!activity.isFinishing)//
            d.show()
    }

    /**
     * 根据type更新数据
     * @param types List<Int>
     */
    fun oneKeyUpdate(activity: Activity, types: List<Int>, back: (() -> Unit)?=null) {
        val textDialog = ProgressTextDialog(activity, "正在更新", false)
        runOnPool {
            types.forEach {
                val result = ResultBox<Boolean>()
                val onUpdate = object : OnUpdate {
                    override fun invoke(p1: Int, p2: String) {
                        when (p1) {
                            Color.GREEN ->
                                textDialog.appendlnGreen(p2)
                            Color.RED -> {
                                textDialog.appendlnRed(p2)
                            }
                            Color.YELLOW ->
                                textDialog.appendlnAmber(p2)

                            else ->
                                textDialog.appendln(p2)
                        }
                    }
                }

                when (it) {
                    0 -> {
                        textDialog.appendlnGreen("正在获取：全局指令")
                        syncGlobalInst(onUpdate) { b ->
                            result.setAndNotify(b)
                        }
                        if (result.blockedGet() == true) {
                            textDialog.appendlnGreen("成功")
                        } else {
                            textDialog.appendlnRed("失败,可至帮助进行反馈")
                        }
                    }
                    1 -> {
                        textDialog.appendlnGreen("正在获取：应用内指令")
                        syncInAppInst(onUpdate) { b -> result.setAndNotify(b) }
                        if (result.blockedGet() == true) {
                            textDialog.appendlnGreen("成功")
                        } else {
                            textDialog.appendlnRed("失败,可至帮助进行反馈")
                        }
                    }
                    2, 3, 4 -> {//marked data
                        textDialog.appendlnGreen("正在获取：" + arrayOf("标记联系人", "标记应用", "标记功能")[it - 2])
                        syncMarkedData(onUpdate, getTypes(it), markedLastKeyId[it - 2]) { b ->
                            result.setAndNotify(b)
                        }
                        if (result.blockedGet() == true) {
                            textDialog.appendlnGreen("成功")
                        } else {
                            textDialog.appendlnRed("失败,可至帮助进行反馈")
                        }
                    }
                    5 -> {//ad
                        textDialog.appendlnGreen("正在获取：标记广告")
                        syncMarkedAd(onUpdate) { b -> result.setAndNotify(b) }
                        if (result.blockedGet() == true) {
                            textDialog.appendlnGreen("成功")
                        } else {
                            textDialog.appendlnRed("失败,可至帮助进行反馈")
                        }
                    }
                }
            }
            textDialog.appendlnGreen("更新完成")
            textDialog.finish()
            back?.invoke()
        }
    }


    private val markedLastKeyId = arrayOf(
            R.string.key_last_sync_marked_contact_date,
            R.string.key_last_sync_marked_app_date,
            R.string.key_last_sync_marked_open_date
    )

    private fun getTypes(t: Int): Array<String> {
        return when (t) {
            2 -> arrayOf(MarkedData.MARKED_TYPE_CONTACT)
            3 -> arrayOf(MarkedData.MARKED_TYPE_APP)
            4 -> arrayOf(MarkedData.MARKED_TYPE_SCRIPT_JS, MarkedData.MARKED_TYPE_SCRIPT_LUA)
            else -> arrayOf()
        }

    }

    private fun check(lastDateInfo: LastDateInfo): Pair<List<Int>, String> {
        val sp = SpHelper(GlobalApp.APP)
        val needUpdateTypes = mutableListOf<Int>()
        val needUpdateS = StringBuilder()
        arrayOf(arrayOf("全局指令", lastDateInfo.instGlobal, R.string.key_last_sync_global_date)//1
                , arrayOf("应用内指令", lastDateInfo.instInApp, R.string.key_last_sync_in_app_date)//2
                , arrayOf("标记通讯录", lastDateInfo.markedContact, R.string.key_last_sync_marked_contact_date)//3
                , arrayOf("标记应用", lastDateInfo.markedApp, R.string.key_last_sync_marked_app_date)//4
                , arrayOf("标记功能", lastDateInfo.markedOpen, R.string.key_last_sync_marked_open_date)//5
                , arrayOf("标记广告", lastDateInfo.markedAd, R.string.key_last_sync_marked_ad_date) //6
        ).withIndex().forEach { kv ->
            val it = kv.value
            val lastDate = it[1] as Long? ?: -1L
            val lastUpdate = sp.getLong(it[2] as Int)
            val isOutDate = lastDate > lastUpdate
            if (isOutDate) {
                needUpdateTypes.add(kv.index)
                needUpdateS.appendln(it[0])
            }
        }
        return Pair(needUpdateTypes, needUpdateS.toString())
    }


    /**
     * 同步全局命令
     * @param back (Boolean) -> Unit
     */
    fun syncGlobalInst(onUpdate: OnUpdate? = null, back: (Boolean) -> Unit) {
        NetHelper.postJson<List<ActionNode>>(ApiUrls.SYNC_GLOBAL_INST, BaseRequestModel(""),
                type = NetHelper.ActionNodeListType) { _, bean ->
            if (bean != null) {
                if (bean.isOk()) {
                    val list = bean.data
                    if (list != null) {
                        DaoHelper.updateGlobalInst(list, onUpdate).also {
                            if (it) {
                                SpHelper(GlobalApp.APP).set(R.string.key_last_sync_global_date, System.currentTimeMillis())
                                DAO.clear()
                                ParseEngine.updateGlobal()
                                back.invoke(true)
                            } else {
                                GlobalApp.toastShort("同步失败")
                                back.invoke(false)
                            }
                        }
                    } else {
                        GlobalLog.err("code: GI57")
                        GlobalApp.toastShort(R.string.text_error_occurred)
                        back.invoke(false)
                    }
                } else {
                    GlobalApp.toastShort(R.string.text_net_err)
                    back.invoke(false)
                }
            } else {
                GlobalApp.toastShort(R.string.text_net_err)
                back.invoke(false)
            }
        }
    }

    /**
     * 同步App内指令
     * @param back (Boolean)->Unit
     */
    fun syncInAppInst(onUpdate: OnUpdate? = null, back: (Boolean) -> Unit) {
        NetHelper.postJson<List<ActionNode>>(ApiUrls.SYNC_IN_APP_INST,
                BaseRequestModel(AdvanAppHelper.getPkgList()),
                type = NetHelper.ActionNodeListType) { _, bean ->
            if (bean != null) {
                if (bean.isOk()) {
                    val list = bean.data
                    if (list != null) {
                        DaoHelper.updateInAppInst(list, onUpdate).also {
                            if (it) {
                                SpHelper(GlobalApp.APP).set(R.string.key_last_sync_in_app_date,
                                        System.currentTimeMillis())
                                DAO.clear()
                                ParseEngine.updateInApp()
                                back.invoke(true)
                            } else {
                                GlobalApp.toastShort("同步失败")
                                back.invoke(false)
                            }
                        }
                    } else {
                        GlobalLog.err("code: GI57")
                        GlobalApp.toastShort(R.string.text_error_occurred)
                        back.invoke(false)
                    }
                } else {
                    GlobalApp.toastShort(R.string.text_net_err)
                    back.invoke(false)
                }

            } else {
                GlobalApp.toastShort(R.string.text_net_err)
                back.invoke(false)
            }

        }
    }

    /**
     * 同步MarkedData
     * @param types Array<String>
     * @param lastKeyId Int
     * @param back (Boolean) -> Unit
     */
    fun syncMarkedData(onUpdate: OnUpdate? = null, types: Array<String>, lastKeyId: Int, back: (Boolean) -> Unit) {
        val syncData = TextHelper.arr2String(types)
        val requestModel = BaseRequestModel(syncData)

        NetHelper.postJson<List<MarkedData>>(ApiUrls.SYNC_MARKED, requestModel,
                type = NetHelper.MarkedDataListType) { _, bean ->
            if (bean?.isOk() == true) {
                DaoHelper.updateMarkedData(onUpdate, types, bean.data ?: emptyList())
                SpHelper(GlobalApp.APP).set(lastKeyId, System.currentTimeMillis())
                DAO.clear()
                back.invoke(true)
            } else {
                GlobalApp.toastShort(bean?.message ?: "未知错误")
                back.invoke(false)
            }
        }
    }

    /**
     * 同步标记广告
     * @param back (Boolean) -> Unit
     */
    fun syncMarkedAd(onUpdate: OnUpdate? = null, back: (Boolean) -> Unit) {

        val syncPkgs = AdvanAppHelper.getPkgList()
        NetHelper.postJson<List<AppAdInfo>>(ApiUrls.SYNC_APP_AD, BaseRequestModel(syncPkgs), type = object
            : TypeToken<ResponseMessage<List<AppAdInfo>>>() {}.type) { _, bean ->
            if (bean != null) {
                if (bean.isOk()) {
                    //
                    DaoHelper.updateAppAdInfo(bean.data ?: emptyList(), onUpdate)
                    SpHelper(GlobalApp.APP).set(R.string.key_last_sync_marked_ad_date, System.currentTimeMillis())
                    AdKillerService.update()
                    DAO.clear()
                    back.invoke(true)
                } else {
                    GlobalApp.toastShort(bean.message)
                    back.invoke(false)
                }
            } else {
                GlobalApp.toastShort("出错")
                back.invoke(false)
            }
        }
    }
}

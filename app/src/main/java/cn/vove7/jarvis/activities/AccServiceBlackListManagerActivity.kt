package cn.vove7.jarvis.activities

import android.support.v4.app.Fragment
import android.view.Menu
import cn.vove7.common.accessibility.AccessibilityApi
import cn.vove7.common.app.GlobalApp
import cn.vove7.common.utils.ThreadPool
import cn.vove7.executorengine.helper.AdvanAppHelper
import cn.vove7.jarvis.R
import cn.vove7.jarvis.activities.base.OneFragmentActivity
import cn.vove7.jarvis.adapters.SimpleListAdapter
import cn.vove7.jarvis.adapters.ViewModel
import cn.vove7.jarvis.fragments.SimpleListFragment
import cn.vove7.jarvis.tools.QueryListener
import cn.vove7.jarvis.tools.SearchActionHelper
import cn.vove7.vtp.app.AppInfo
import cn.vove7.vtp.sharedpreference.SpHelper

/**
 * # AccServiceBlackListManagerActivity
 * 无障碍黑名单管理
 * @author Administrator
 * 2018/12/2
 */
@Deprecated("无用")
class AccServiceBlackListManagerActivity : OneFragmentActivity() {

    override var fragments: Array<Fragment> = arrayOf(
            SFragment()
    )

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        val searchItem = menu!!.findItem(R.id.menu_search)
        SearchActionHelper(searchItem) {
            (fragments[0] as QueryListener).onQuery(it)
        }
        return true
    }


    class SFragment : SimpleListFragment<AppInfo>(), QueryListener {

        @Synchronized
        override fun onQuery(text: String) {
            startRefreshing()
            if (text == "") {
                refresh()
                return
            }
            ThreadPool.runOnCachePool {
                val tmp = dataSet.filter {
                    it.title?.contains(text, ignoreCase = true) == true
                }
                clearDataSet()
                dataSet.addAll(tmp)
                notifyLoadSuccess(true)
            }
        }

        val sp by lazy { SpHelper(GlobalApp.APP) }

        private val blackSet: HashSet<String> =
            HashSet(sp.getStringSet("acc_black_list")
                ?: emptySet())

        override val itemCheckable: Boolean = true

        override fun unification(data: AppInfo): ViewModel<AppInfo>? {
            return ViewModel(title = data.name, subTitle = data.packageName,
                    icon = data.getIcon(context!!), extra = data,
                    checked = blackSet.contains(data.packageName))
        }

        override val itemClickListener: SimpleListAdapter.OnItemClickListener<AppInfo> =
            object : SimpleListAdapter.OnItemClickListener<AppInfo> {
                override fun onClick(holder: SimpleListAdapter.VHolder?, pos: Int, item: ViewModel<AppInfo>) {
                    holder?.checkBox?.toggle()
                    item.checked = holder?.checkBox?.isChecked ?: false //这里手动改状态
                    onItemCheckedStatusChanged(holder, item, holder?.checkBox?.isChecked ?: false)
                }

                override fun onItemCheckedStatusChanged(holder: SimpleListAdapter.VHolder?, item: ViewModel<AppInfo>, isChecked: Boolean) {
                    if (isChecked)
                        blackSet.add(item.extra.packageName)
                    else
                        blackSet.remove(item.subTitle)
//                    AccessibilityApi.accessibilityService?.loadBlackList(blackSet)
                    sp.set("acc_black_list", blackSet)
                }
            }

        override fun onGetData(pageIndex: Int) {
            ThreadPool.runOnCachePool {
                notifyLoadSuccess(AdvanAppHelper.APP_LIST.values.toList(), true)
            }
        }
    }
}
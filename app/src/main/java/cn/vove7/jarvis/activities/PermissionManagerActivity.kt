package cn.vove7.jarvis.activities

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import cn.vove7.common.app.GlobalApp
import cn.vove7.jarvis.R
import cn.vove7.jarvis.activities.PermissionManagerActivity.ManageFragment.PermissionStatus.Companion.permissions
import cn.vove7.jarvis.activities.base.OneFragmentActivity
import cn.vove7.jarvis.adapters.RecAdapterWithFooter
import cn.vove7.jarvis.fragments.VListFragment
import cn.vove7.vtp.runtimepermission.PermissionUtils

/**
 * 权限管理
 */
class PermissionManagerActivity : OneFragmentActivity() {
    override var fragments: Array<Fragment> = arrayOf(ManageFragment())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //执行时消息
        if (intent.hasExtra("pName")) {
            GlobalApp.toastShort(String.format(getString(R.string.text_operation_need_permission)
                    , intent.getStringExtra("pName")))
        }
    }

    class ManageFragment : VListFragment() {
        //        lateinit var permissionList: List<PermissionStatus>
        //        lateinit var adapter: BaseAdapter
        override fun clearDataSet() {
        }


        override fun initView(contentView: View) {
            PermissionStatus.refreshStatus()
            adapter = buildAdapter()
            recyclerView.isVerticalScrollBarEnabled = false
        }

        override fun onGetData(pageIndex: Int) {
            PermissionStatus.refreshStatus()
            notifyLoadSuccess(true)
            adapter.hideFooterView()
        }

        private fun buildAdapter(): RecAdapterWithFooter<Holder> {
            return object : RecAdapterWithFooter<Holder>() {

                override fun itemCount(): Int = permissions.size

                override fun getItem(pos: Int): PermissionStatus? {
                    return permissions[pos]
                }

                override fun onCreateHolder(parent: ViewGroup, viewType: Int): Holder {
                    val view = layoutInflater.inflate(R.layout.item_of_permission_list, parent, false)
                    return Holder(view)
                }

                override fun onBindView(holder: Holder, position: Int, it: Any?) {
                    val item = it as PermissionStatus

                    holder.title.text = item.permissionName
                    if (item.desc == "") {
                        holder.subtitle.visibility = View.GONE
                    } else {
                        holder.subtitle.visibility = View.VISIBLE
                        holder.subtitle.text = item.desc
                    }
                    when (item.isOpen) {
                        true -> {
                            holder.open.text = getString(R.string.text_opened)
                            holder.open.setTextColor(resources.getColor(R.color.status_green))
                            holder.title.setTextColor(resources.getColor(R.color.primary_text))
                        }
                        else -> {
                            holder.open.text = getString(R.string.text_to_open)
                            holder.open.setTextColor(resources.getColor(R.color.red_500))
                            holder.title.setTextColor(resources.getColor(R.color.red_500))
                        }
                    }
                    holder.itemView.setOnClickListener {
                        if (!item.isOpen) {
                            when {
                                item.permissionName == "悬浮窗" ->
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        PermissionUtils.requestDrawOverlays(activity!!, 0)
                                    }
                                item.permissionName == "无障碍" ->
                                    PermissionUtils.gotoAccessibilitySetting(activity!!)
                                else ->
                                    PermissionUtils.autoRequestPermission(activity!!,
                                            item.permissionString, position)
                            }
                        }
                    }
                }
            }
        }


        override fun onResume() {
            super.onResume()
            refreshStatus()
        }

        private fun refreshStatus() {
            PermissionStatus.refreshStatus()
            adapter.notifyDataSetChanged()
        }

        override fun onRequestPermissionsResult(requestCode: Int, perm: Array<out String>, grantResults: IntArray) {
            if (PermissionUtils.isAllGranted(grantResults)) {
                permissions[requestCode].isOpen = true
                adapter.notifyDataSetChanged()
            }
        }

        class Holder(view: View) : RecAdapterWithFooter.RecViewHolder(view, null) {
            val title = view.findViewById<TextView>(R.id.title)
            val subtitle = view.findViewById<TextView>(R.id.subtitle)
            val open = view.findViewById<TextView>(R.id.open)
        }

        class PermissionStatus(
                val permissionString: Array<String>,
                val permissionName: String,
                val desc: String,
                var isOpen: Boolean = false
        ) {
            companion object {

                val permissions = listOf(
                        PermissionStatus(arrayOf("android.permission.BIND_ACCESSIBILITY_SERVICE"), "无障碍", "操作界面"),
                        PermissionStatus(arrayOf("android.permission.SYSTEM_ALERT_WINDOW"), "悬浮窗", "显示全局对话框"),
                        PermissionStatus(arrayOf("android.permission.READ_CONTACTS"), "联系人", "用于检索联系人"),
                        PermissionStatus(arrayOf("android.permission.CALL_PHONE"), "电话", "用于拨打电话"),
                        PermissionStatus(arrayOf("android.permission.RECORD_AUDIO"), "录音", "用于语音识别"),
                        PermissionStatus(arrayOf("android.permission.ACCESS_NETWORK_STATE"), "获取网络状态", "用于获取网络状态"),
                        PermissionStatus(arrayOf("android.permission.INTERNET"), "网络", ""),
                        PermissionStatus(arrayOf("android.permission.READ_PHONE_STATE"), "读取设备状态", ""),
                        PermissionStatus(arrayOf("android.permission.WRITE_EXTERNAL_STORAGE"), "写SD卡", ""),
                        PermissionStatus(arrayOf("android.permission.FLASHLIGHT"), "闪光灯", "打开闪光灯"),
//                        PermissionStatus(arrayOf("android.permission.BLUETOOTH", "android.permission.BLUETOOTH_ADMIN"),
//                                "蓝牙", "打开蓝牙"),
                        PermissionStatus(arrayOf("android.permission.CAMERA"), "相机", "打开闪光灯")
                        //                    PermissionStatus("android.permission.VIBRATE", "震动", ""),
                )

                fun refreshStatus() {
                    val context = GlobalApp.APP
                    permissions.forEach {
                        it.isOpen = when {
                            it.permissionName == "悬浮窗" -> Build.VERSION.SDK_INT < Build.VERSION_CODES.M || PermissionUtils.canDrawOverlays(context)
                            it.permissionName == "无障碍" -> PermissionUtils.accessibilityServiceEnabled(context)
                            else -> PermissionUtils.isAllGranted(context, it.permissionString)
                        }
                    }
                }
            }

        }

    }
}
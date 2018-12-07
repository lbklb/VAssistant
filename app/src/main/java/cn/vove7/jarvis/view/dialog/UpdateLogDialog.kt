package cn.vove7.jarvis.view.dialog

import android.content.Context
import android.graphics.Typeface
import cn.vove7.common.view.editor.MultiSpan
import com.afollestad.materialdialogs.callbacks.onDismiss

/**
 * # UpdateLogDialog
 *
 * @author Administrator
 * 2018/10/28
 */
class UpdateLogDialog(context: Context, onDismiss: (() -> Unit)? = null) {
    init {
        val d = ProgressTextDialog(context, "更新日志", false)
        d.dialog.onDismiss {
            onDismiss?.invoke()
        }
        logs.forEach {
            d.appendln()
            d.appendln(MultiSpan(context, it.first, fontSize = 20, typeface = Typeface.BOLD).spanStr)
            d.appendln(it.second)
        }
        d.scrollToTop()
        d.finish()
    }

    private val logs: List<Pair<String, String>>
        get() = listOf(
                Pair("1.5.8","优化无障碍服务\n" +
                        "提示音问题\n" +
                        "优化二维码识别"),
                Pair("1.5.7","修复speak后开启识别及其他一些问题"),
                Pair("1.5.6", "修复一加5/5t 安卓P版本在最近任务页面卡顿问题\n" +
                        "移除青云客对话系统\n" +
                        "修复speak后无法长语音问题\n" +
                        "加入无障碍黑名单[实验室/其他]"),
                Pair("1.5.5", "支持[语音唤醒]和[长语音]同时开启\n" +
                        "加入长语音定时关闭\n" +
                        "加入语音悬浮窗位置设置[设置/语音面板]"),
                Pair("1.5.4", "修复任何开启长语音时，语音合成结束后，识别开启问题"),
                Pair("1.5.3", "加入长语音（支持连续说出命令）[设置/语音识别/长语音]\n" +
                        "修复开启提示音部分情况不识别问题"),
                Pair("1.5.2", "修复插件安装失败"),
                Pair("1.5.1", "加入提示音\n" +
                        "加入插件管理 可扩展更多功能\n" +
                        "修复未安装标记应用匹配问题\n" +
                        "修复未打开无障碍，使用聊天问题\n" +
                        "修复一些问题\n" +
                        "添加api：\n" +
                        "  system.getContactByName()"),
                Pair("1.5.0", "开放脚本编辑、新建指令、指令分享、新建标记等功能"),
                Pair("1.4.1", "修复只翻译第一行文本\n" +
                        "修复部分三星拨打电话权限问题"),
                Pair("1.4.0", "修复7.0版本下脚本执行崩溃问题"),
                Pair("1.3.8", "文字提取支持翻译\n修复一些导致崩溃的问题"),
                Pair("1.3.7", "修复卡顿问题\n" +
                        "加快广告跳过速度"),
                Pair("1.3.6", "在其他App内由于麦克风占用关闭唤醒时, 熄屏开启唤醒\n" +
                        "加入长按耳机中键唤醒设置\n" +
                        "添加快捷方式，其他应用可通过此方式唤醒(如[悬浮菜单]中选择快捷方式唤醒)\n" +
                        "悬浮面板显示语音回复内容"),
                Pair("1.3.5", "修复引擎未就识别导致崩溃\n" +
                        "添加设置长按延时\n" +
                        "修复App内指令设为全局失败\n" +
                        "添加[快捷唤醒]桌面小部件\n" +
                        "静音功能适配\n" +
                        "添加api：\n" +
                        "  ViewFinder.waitHide()"),
                Pair("1.3.4", "支持选卡拨号\n" +
                        "支持VSCode将代码发送至App新建指令\n" +
                        "支持VSCode将文本复制至手机剪切板\n" +
                        "添加api\n" +
                        "  system.simCount\n" +
                        "  system.contacts\n" +
                        "  system.saveMarkedContact\n" +
                        "  system.saveMarkedApp\n" +
                        "  system.call(phone,simId)\n"),
                Pair("1.3.3", "修复低于安卓8.0执行system函数崩溃问题"),
                Pair("1.3.2", "修复语音唤醒定时问题"),
                Pair("1.3.1", "加入省电模式(必关闭语音唤醒)\n" +
                        "加入快捷屏幕助手[实验室]（设为默认辅助应用，可长按HOME键触发，部分机型需手动设置）\n" +
                        "  -屏幕识别\n" +
                        "  -文字提取\n" +
                        "  -分享屏幕\n" +
                        "  -二维码/条码识别\n" +
                        "无障碍/语音唤醒状态改通知栏通知\n" +
                        "修复若干问题\n" +
                        "添加api\n" +
                        "  system.batteryLevel\n" +
                        "  system.isCharging()"),
                Pair("1.3.0", "解决语音唤醒麦克风占用问题\n修复若干问题"),
                Pair("1.2.10", "支持多个用户唤醒词\n" +
                        "修复识别时仍能使用唤醒词唤醒问题\n" +
                        "修复开启无障碍崩溃问题"),
                Pair("1.2.9", "加入用户唤醒词\n" +
                        "添加标志runtime.userInterrupt\n" +
                        "优化当前App/Activity判断"),
                Pair("1.2.8", "- 数据更新方式换为增量更新\n" +
                        "- 支持网页内文字提取\n" +
                        "- 支持语音唤醒后无间断说出命令（响应词打开则无效）\n" +
                        "- 修改后台控制音乐逻辑\n" +
                        "- 加入结束词 (实验室)\n" +
                        "- 加入执行反馈设置\n" +
                        "- 完善自动开启无障碍\n" +
                        "- 添加基础引导\n" +
                        "- 修复关闭应用指令\n" +
                        "- 命令修剪结束词\n" +
                        "- 添加api\n" +
                        "system.sendSMS(phone: String, content: String)\n" +
                        "system.getLaunchIntent(pkg:String)\n" +
                        "system.getPhoneByName(name: String)\n" +
                        "runtime.focusView\n" +
                        "ViewNode.appendText()\n" +
                        "ViewNode.globalClick()\n" +
                        "ViewFinder.containsDesc(...)"),
                Pair("1.2.6", "加入指令优先级设置   (指令详情菜单) ps: 指令列表按优先级排列\n" +
                        "加入连续对话 (实验室)\n" +
                        "修复若干问题"),
                Pair("1.2.5", "修复图灵机器人调用问题\n" +
                        "修复解析日期‘x小时’后错误\n" +
                        "其他问题"),
                Pair("1.2.4", "加入对话系统：图灵机器人 (/实验室)"),
                Pair("1.2.3", "修复部分机型识别后崩溃"),
                Pair("1.2.2", "加入App自动检查更新\n" +
                        "加入对话系统\n" +
                        "加入语音唤醒自动休眠后亮屏后自动开启\n" +
                        "加入在播放语音合成时，长按音量下可停止播放"),
                Pair("1.2.1_0", "fix 7.1.1及以下设备识别结束崩溃问题\n" +
                        "移除对[5.0-6.0)的支持\n" +
                        "fix 部分设备截屏崩溃问题"),
                Pair("1.2.1", "在[文字提取]加入分词功能\n" +
                        "离线命令词，识别联系人，和应用更准确\n" +
                        "支持阿拉伯数字与中文模糊匹配\n" +
                        "加入 关于/更新日志"),
                Pair("1.2.0", "fix 语音唤醒响应词结束后，播放音乐\n" +
                        "添加非充电状态下，语音唤醒自动休眠\n" +
                        "支持有线耳机耳麦唤醒和语音识别\n" +
                        "支持多唤醒词，支持的唤醒词(唤醒即执行)： 播放,停止,暂停,上一首,下一首,打开手电筒,关闭手电筒,截屏分享,文字提取)\n" +
                        "(自定义过唤醒词的，需要手动恢复默认，如需自定义，可以将上面唤醒词和你的自定义在获取唤醒词一起导出）\n" +
                        "添加耳机中键唤醒\n" +
                        "添加代码编辑器快速插入 require 'accessibility'\n" +
                        "tips: 蓝牙快捷键(若支持)可触发默认语音助手"),
                Pair("1.1.8", "代码编辑器添加插入声明无障碍模式\n" +
                        "添加系统Api screenOn screenOff sendKey\n" +
                        "修复 在speakSync 音量静音时，显示toast后 回调失败问题\n" +
                        "其他问题"),
                Pair("1.1.7_5", "fix 指令设置初始化失败\n" +
                        "新增 运行时 状态栏通知显示命令"),
                Pair("1.1.7_3", "\n" +
                        "添加日期解析 'x天后'\n" +
                        "fix some bug"),
                Pair("1.1.7_2",
                        "修复指令同步失败\n" +
                                "修复打开标记功能\n" +
                                "自启无障碍同时保持其他服务\n" +
                                "若干其他其他问题"),
                Pair("1.1.7",
                        "支持指令多参数\n" +
                                "添加自动开启无障碍服务(设置/其他 需手动开启)\n" +
                                "添加指令 创建闹钟和日历事件（均此版本可用）\n" +
                                "添加api androRuntime (终端/Root命令相关)\n" +
                                "添加api parseDateText (解析日期文本 ;Fx其他页)\n" +
                                "更新时数据显示更新进度和内容\n" +
                                "解决误认输入法为当前activity")
                ,
                Pair("1.1.6",
                        "增加创建日历事件、闹钟api (系统函数)\n" +
                                "修复语音合成通道设置问题及some bug")
                ,
                Pair("1.1.5", "fix 唤醒响应词，后台音乐未播放时，识别结束后音乐响起问题\n" +
                        "完成备份功能\n" +
                        "自定义唤醒时响应词\n" +
                        "加入语音合成选择音频输出通道\n" +
                        "移除对x86的支持\n" +
                        "修复若干问题")
                ,
                Pair("1.1.4_1",
                        "修复部分机型无障碍错误崩溃\n" +
                                "修复部分机型闪光灯打开失败\n" +
                                "修复其他若干问题")
                ,
                Pair("1.1.4",
                        "加入数据自动更新和一键更新(高级中)\n" +
                                "加入网络api（示例参考js编辑器中的'天气.js'）\n" +
                                "加入屏幕文字提取(同步最新数据后，使用文字提取 )\n" +
                                "加入指令详情代码复制")
                , Pair("1.1.0",
                "完成 代码编辑器\n" +
                        "自定义唤醒词\n" +
                        "修复截屏后投屏图标不消失\n" +
                        "加入云解析\n" +
                        "增加本地解析失败逻辑")
                , Pair("1.0.4",
                "- 加入充电时自动开启语音唤醒\n" +
                        "- 添加shortcut功能 可添加 唤醒/指令 桌面快捷方式\n" +
                        "- 可设置解析失败，自动执行smart打开操作，详情见 高级/命令解析\n")
        )
}
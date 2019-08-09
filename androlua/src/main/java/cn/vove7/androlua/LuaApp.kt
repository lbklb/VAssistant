package cn.vove7.androlua

import android.content.Context
import cn.vove7.androlua.luabridge.LuaUtil
import cn.vove7.common.app.GlobalApp
import cn.vove7.common.app.GlobalLog
import cn.vove7.common.app.log
import cn.vove7.common.utils.ThreadPool.runOnPool
import java.io.File
import java.io.IOException


/**
 * LuaApp
 *
 *
 * Created by Vove on 2018/7/31
 */
class LuaApp {
    companion object {
        /**
         * 初始化lua资源
         * @param context Context
         */
        fun init(context: Context, update: Boolean) {
            if(!update) return
            runOnPool {
                initAsset(context)
            }
        }

        private fun initAsset(context: Context) {
            try {
                val fp = context.filesDir.absolutePath
                context.assets!!.list("lua_requires")!!
                        .forEach {
                            try {
                                LuaUtil.assetsToSD(context, "lua_requires/$it", "$fp/$it")
                            } catch (e: IOException) {
                                e.log()
                                e.printStackTrace()
                            }
                        }
            } catch (e: Throwable) {
                e.log()
                GlobalApp.toastError("Lua初始化失败，可将日志反馈至讨论群")
            }
        }

    }
}
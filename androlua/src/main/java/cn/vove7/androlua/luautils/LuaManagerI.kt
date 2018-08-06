package cn.vove7.androlua.luautils

import android.content.Context
import cn.vove7.common.BridgeManager
import com.luajava.LuaException
import dalvik.system.DexClassLoader
import java.util.*

/**
 * LuaManagerI
 * 负责管理
 *
 *
 * Created by Vove on 2018/8/1
 */
interface LuaManagerI {
    val librarys: HashMap<String, String>

    val classLoaders: ArrayList<ClassLoader>

    var bridgeManager: BridgeManager?
    val app: Context

    @Throws(LuaException::class)
    fun loadDex(path: String): DexClassLoader

    fun regGc(obj: LuaGcable)

    fun gc(obj: LuaGcable)

    fun stop()

    fun handleError(err: String)

    fun handleError(e: Exception)

    fun handleMessage(l: Int, msg: String)

    fun log(log: String)

    companion object {

        const val L = 0
        const val W = 1//Prompt
        const val E = 2
    }
}
package cn.vove7.common.bridges

import android.graphics.Bitmap
import cn.vove7.common.NotSupportException
import cn.vove7.common.app.GlobalApp
import cn.vove7.common.app.GlobalLog
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import top.zibin.luban.Luban
import java.io.File
import java.io.FileOutputStream
import java.util.*


/**
 * # UtilBridge
 *
 * @author Administrator
 * 2018/10/6
 */
object UtilBridge {

    fun compressImage(file: String): File = UtilBridge.compressImage(File(file))

    /**
     * 压缩图片，使用Luban
     * @param file File
     * @return File
     */
    fun compressImage(file: File): File {
        return Luban.with(GlobalApp.APP).load(file).ignoreBy(100)
                .setTargetDir(file.parent)
                .get()[0] ?: file
    }

    fun bitmap2File(bitmap: Bitmap, fullPath: String): File? {
        return try {
            File(fullPath).apply {
                if (!parentFile.exists())
                    parentFile.mkdirs()
                FileOutputStream(this).use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        } catch (se: SecurityException) {
            GlobalApp.toastError("无存储权限")
            null
        } catch (e: Exception) {
            GlobalLog.err(e)
            GlobalLog.err("bitmap2File 保存到失败")
            null
        }
    }

    /**
     * json解析为Map
     * @param json String?
     * @return Map<String, Any?>?
     */
    fun parseJson(json: String?): Map<String, Any?>? {
        json ?: return null
        val jobj = JsonParser().parse(json).asJsonObject

        return toMap(jobj)
    }

    /**
     * 将JSONObjec对象转换成Map-List集合
     * @param json
     * @return
     */
    private fun toMap(json: JsonObject): Map<String, Any> {
        val map = HashMap<String, Any>()
        val entrySet = json.entrySet()
        val iter = entrySet.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            val key = entry.key
            val value = entry.value
            map[key as String] = when (value) {
                is JsonArray -> toList(value)
                is JsonObject -> toMap(value)
                else -> value
            }
        }
        return map
    }

    /**
     * 将JSONArray对象转换成List集合
     * @param json
     * @return
     */
    private fun toList(json: JsonArray): List<Any> {
        val list = ArrayList<Any>()
        for (i in 0 until json.size()) {
            val value = json.get(i)
            list.add(when (value) {
                is JsonArray -> toList(value)
                is JsonObject -> toMap(value)
                else -> value
            })
        }
        return list
    }

    /**
     * 当指令无法完成请求时，抛出该异常
     */
    fun notSupport() {
        throw NotSupportException()
    }

}
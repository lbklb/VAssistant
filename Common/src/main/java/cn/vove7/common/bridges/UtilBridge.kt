package cn.vove7.common.bridges

import android.graphics.Bitmap
import cn.vove7.common.app.GlobalApp
import cn.vove7.common.app.GlobalLog
import cn.vove7.vtp.log.Vog
import java.io.File
import java.io.FileOutputStream
import android.content.Intent
import android.net.Uri


/**
 * # UtilBridge
 *
 * @author Administrator
 * 2018/10/6
 */
object UtilBridge {

    fun bitmap2File(bitmap: Bitmap, fullPath: String): File? {//保存到本地
        Vog.d("bitmap2File ---> $fullPath")
        return try {
            File(fullPath).apply {
                if (!parentFile.exists())
                    parentFile.mkdirs()
                FileOutputStream(this).use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                GlobalApp.APP.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.fromFile(this)))
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

}
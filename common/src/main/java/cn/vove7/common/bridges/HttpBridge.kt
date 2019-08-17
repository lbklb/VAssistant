package cn.vove7.common.bridges

import cn.vove7.common.app.GlobalApp
import cn.vove7.common.app.GlobalLog
import cn.vove7.common.model.ResultBox
import cn.vove7.vtp.log.Vog
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * # HttpBridge
 *
 * @author Administrator
 * 2018/10/12
 */
object HttpBridge {
    var timeout = 5L

    fun get(url: String): String? {
        return get(url, null)
    }

    fun get(url: String, params: Map<String, Any>?): String? {
        println("get ---> $url $params")
        val client = OkHttpClient.Builder()
                .readTimeout(timeout, TimeUnit.SECONDS).build()
        val request = Request.Builder().url(url)
                .get().also {
                    params?.forEach { p ->
                        it.addHeader(p.key, p.value.toString())
                    }
                }
                .build()
        val call = client.newCall(request)
        return call(call)
    }

    fun getAsPc(url: String): String? {
        return getAsPc(url, null)
    }

    fun getAsPc(url: String, params: Map<String, Any>?): String? {
        Vog.d("get ---> $url $params")
        val client = OkHttpClient.Builder()
                .readTimeout(timeout, TimeUnit.SECONDS).build()
        val request = Request.Builder().url(url)
                .get().also {
                    params?.forEach { p ->
                        it.addHeader(p.key, p.value.toString())
                    }
                }
                .removeHeader("User-Agent")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; rv:26.0) Gecko/20100101 Firefox/26.0")
                .build()
        val call = client.newCall(request)
        return call(call)
    }

    fun postJson(url: String, jsonP: Map<String, Any>?): String? {
        return postJson(url, Gson().toJson(jsonP))
    }

    fun postJson(url: String, json: String?): String? {
//        Vog.d("get ---> $url \n$json")
        println("get ---> $url \n$json")
        val client = OkHttpClient.Builder()
                .readTimeout(timeout, TimeUnit.SECONDS).build()
        val requestBody = FormBody.create(MediaType
                .parse("application/json; charset=utf-8"), json ?: "")

        val request = Request.Builder().url(url)
                .post(requestBody)
                .build()
        val call = client.newCall(request)
        return call(call)
    }

    fun post(url: String): String? {
        return post(url, null)
    }


    //postForm
    fun post(url: String, params: Map<String, Any>?): String? {
//        Vog.d("post ---> $url $params")
        val client = OkHttpClient.Builder()
                .readTimeout(timeout, TimeUnit.SECONDS).build()
        val requestBody = FormBody.Builder().apply {
            params?.forEach { p ->
                add(p.key, p.value.toString())
            }
        }.build()

        val request = Request.Builder().url(url)
                .post(requestBody)
                .build()
        val call = client.newCall(request)
        return call(call)
    }

    /**
     * 网络同步请求
     * @param call Call
     * @return String?
     * @throws IOException
     */
    private fun call(call: Call): String? {
        val result = ResultBox<String?>()
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                GlobalLog.err("请求失败: " + e.message)
                GlobalApp.toastError("网络请求失败")
                result.setAndNotify(null)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {//响应成功更新UI
                if (response.isSuccessful) {
                    val s = response.body()?.string()
                    println("onResponse ---> http bridge $s")
                    result.setAndNotify(s)
                } else {
                    GlobalApp.toastInfo("网络请求失败")
                    GlobalLog.err("网络错误：" + response.message())
                    result.setAndNotify(null)
                }
            }
        })
        return result.blockedGet()
    }

}

interface OnResponse {
    fun onResult(status: Int, content: String?)
}



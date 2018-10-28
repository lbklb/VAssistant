package cn.vove7.jarvis

import cn.vove7.common.bridges.HttpBridge
import cn.vove7.common.datamanager.parse.model.Action
import cn.vove7.common.utils.TextDateParser
import cn.vove7.common.utils.TextHelper
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.Test
import java.lang.Thread.sleep
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        val getParamsReg = "[\\S]*(\\([\\S]*\\))".toRegex()
        arrayOf(
                "setTextById(消息,input)",
                "open(QQ)",
                "sleep"
        ).forEach {

            val mResult = getParamsReg.matchEntire(it)
            var ps: List<String?>
            val c =
                if (mResult != null) {
                    val param = mResult.groupValues[1]
                    ps = param.substring(1, param.length - 1).split(",")
                    it.substring(0, mResult.groups[1]?.range?.first ?: it.length)
                } else {
                    ps = listOf(null)
                    it
                }
            println("$c $ps")

        }

    }

    @Test
    fun simpleTest() {
        println("123".startsWith(""))
    }

    @Test
    fun regTest() {
        //匹配包名 至少一个.
        val r = "[a-zA-Z]+[0-9a-zA-Z_]*(\\.[a-zA-Z]+[0-9a-zA-Z_]*)+".toRegex()
        mapOf(
                Pair("cn.vove7.ok", true),
                Pair("Alipay", false)
        ).forEach {
            println(r.matches(it.key) == it.value)
        }
    }

    @Test
    fun waitTest() {
        val millis = 2000L
        val lock = Object()
        val t = thread {
            val begin = System.currentTimeMillis()
            if (millis < 0) {
                lock.wait()
                println("执行器-解锁")
            } else {
                var end: Long = 0
                synchronized(lock) {
                    lock.wait(millis)
                    end = System.currentTimeMillis()
                    println("$begin -- $end")
                }
                println("执行器-解锁")

                if (end - begin >= millis) {//自动超时 终止执行
                    println("自动解锁")
                }
            }
        }
        while (t.isAlive) sleep(1000)
    }

    @Test
    fun testActionQ() {
        val q = PriorityQueue<Action>()
        q.add(Action("123", ""))
        q.add(Action("456", ""))
        q.add(Action("789", ""))
        q.add(Action(2, "0", ""))
        q.add(Action(-1, "-1", ""))
        while (q.isNotEmpty()) {
            println(q.poll())
        }
    }


    @Test
    fun testArrayIn() {
        arrayOf("媒体音量", "铃声音量", "通知音量").let {
            it.forEach { s ->
                println(it.indexOf(s) in 0..2)
            }
            println(it.indexOf("1") in 0..2)

        }
    }

    @Test
    fun testParseDate() {
        val s = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        arrayOf(
                "中午", "十二点", "八点四十五", "八点半", "晚上八点", "中午12点", "下午2点一刻",
                "明天中午", "后天下午3点", "大后天中午", "昨天下午2:21", "前天下午两点半",
                "周一下午", "下周二八点半", "周日晚上八点",
                "二十号晚上七点", "21号", "二十八号", "下个月十八号上午8点二十三", "十二月25号",
                "12月8号上午8点", "周二一点", "这周五八点", "周五晚上7点半",
                "一小时后", "一个半小时后", "半小时后", "两个半小时后", "45分钟后", "三十二分钟后",
                "两个小时后", "两小时后", "二十小时后",
                "八天后"
        ).forEach {
            //parse
            println(it + "  " + s.format(TextDateParser.parseDateText(it).time))
        }
    }

    @Test
    fun testReg() {
        val m = TextHelper.matchValues("切换应用", "(使?用|打开)%")
        println(m)
    }

    @Test
    fun htmlParse() {
//        val data = HttpBridge.get("https://www.coolapk.com/apk/cn.vove7.vassistant")

        val doc = Jsoup.connect("https://www.coolapk.com/apk/cn.vove7.vassistant").get()

        println(doc.body().getElementsByClass("list_app_info").text())
        println(doc.body().getElementsByClass("apk_left_title_info")[0].html().replace("<br> ","\n"))

    }
}
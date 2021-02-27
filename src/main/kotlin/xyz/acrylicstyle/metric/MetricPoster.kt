@file:JvmName("MetricPosterKt")
package xyz.acrylicstyle.metric

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.plugin.java.JavaPlugin
import util.RESTAPI
import util.ReflectionHelper
import util.promise.UnhandledPromiseException
import java.util.Timer
import java.util.TimerTask
import kotlin.math.roundToInt

@Suppress("unused")
class MetricPoster: JavaPlugin(), Listener {
    private val timer = Timer("Metric Poster Thread")
    private var cpm = 0
    private var queueServer = false
    private var authorization: String? = null
    private var endPoint: String? = null

    override fun onEnable() {
        queueServer = config.getBoolean("queueServer", false)
        authorization = config.getString("authorization") // set same value as AUTHORIZATION in https://github.com/acrylic-style/status.2b2t.jp/blob/master/.env.example
        endPoint = config.getString("endPoint")
        if (authorization == null) logger.warning("authorization is null! things may not work well.")
        if (endPoint == null) return logger.severe("API End Point is null! Plugin will not work.") // example: https://status.2b2t.jp/api/data.json
        timer.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                if (queueServer) {
                    val ppl = server.onlinePlayers.size
                    logger.info("Posting (queue) metric with player count=$ppl")
                    postQueueMetric(ppl)
                } else {
                    val players = server.onlinePlayers.size
                    val tps = getTPS()
                    logger.info("Posting metric with player count=$players, tps=$tps, cpm=$cpm")
                    postMetric(players, tps, cpm)
                }
                cpm = 0
            }
        }, 1000 * 10L, 1000 * 60L)
        server.pluginManager.registerEvents(this, this)
    }

    override fun onDisable() {
        timer.cancel()
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onAsyncPlayerChat(e: AsyncPlayerChatEvent) {
        if (e.recipients.size <= 1 || e.isCancelled) return
        cpm++
    }

    fun postQueueMetric(players: Int) {
        try {
            val res = RESTAPI(
                "$endPoint?queue=true",
                "POST",
                RESTAPI.BodyBuilder()
                    .setRawBody("{\"players\":$players}")
                    .addRequestProperty("Content-Type", "application/json")
                    .addRequestProperty("Authorization", authorization).build()
            ).call().complete()
            if (res.response == null) logger.warning("Something went wrong! Response: " + res.rawResponse)
        } catch (ignore: UnhandledPromiseException) {}
    }

    fun postMetric(players: Int, tps: Double, cpm: Int) {
        val roundedTps = (tps * 100).roundToInt() / (100).toDouble()
        try {
            val res = RESTAPI(
                endPoint!!,
                "POST",
                RESTAPI.BodyBuilder()
                    .setRawBody("{\"tps\":$roundedTps,\"players\":$players,\"cpm\":$cpm}")
                    .addRequestProperty("Content-Type", "application/json")
                    .addRequestProperty("Authorization", authorization).build()
            ).call().complete()
            if (res.response == null) logger.warning("Something went wrong! Response: " + res.rawResponse)
        } catch (ignore: UnhandledPromiseException) {}
    }
}

fun getServerVersion() =
    Bukkit.getServer().javaClass.getPackage().name.replace(".", ",").split(",".toRegex())[3]

fun getNMSPackage() = "net.minecraft.server." + getServerVersion()

fun getNMSClass(clazz: String): Class<*> = Class.forName(getNMSPackage() + "." + clazz)

fun getTPS(): Double {
    return try {
        Bukkit.getTPS()[0]
    } catch (e: NoSuchMethodError) {
        val server = ReflectionHelper.invokeMethodWithoutException(getNMSClass("MinecraftServer"), null, "getServer")
        val rollingAverage = ReflectionHelper.getFieldWithoutException(getNMSClass("MinecraftServer"), server, "tps1")
        ReflectionHelper.invokeMethodWithoutException(getNMSClass("MinecraftServer\$RollingAverage"), rollingAverage, "getAverage") as Double
    }
}

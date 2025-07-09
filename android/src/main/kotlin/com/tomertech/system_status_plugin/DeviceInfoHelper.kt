package com.tomertech.system_status_plugin

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import java.text.DecimalFormat
import kotlin.concurrent.fixedRateTimer

class DeviceInfoPlugin : FlutterPlugin {
    private lateinit var deviceInfoChannel: EventChannel
    private var deviceInfoSink: EventChannel.EventSink? = null
    private var timer: java.util.Timer? = null

    override fun onAttachedToEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        deviceInfoChannel = EventChannel(binding.binaryMessenger, "system_status_plugin/device_info")
        deviceInfoChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                deviceInfoSink = events
                startSendingDeviceInfo(binding.applicationContext)
            }

            override fun onCancel(arguments: Any?) {
                stopSendingDeviceInfo()
                deviceInfoSink = null
            }
        })
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        stopSendingDeviceInfo()
    }

    private fun startSendingDeviceInfo(context: Context) {
        timer = fixedRateTimer("device_info_timer", initialDelay = 0, period = 2000) {
            val info = getDeviceInfo(context)
            deviceInfoSink?.success(info)
        }
    }

    private fun stopSendingDeviceInfo() {
        timer?.cancel()
        timer = null
    }

    private fun getDeviceInfo(context: Context): Map<String, String> {
        val metrics = context.resources.displayMetrics
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val realMetrics = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(realMetrics)

        val totalRam = getTotalRam(context)
        val availRam = getAvailableRam(context)
        val (totalStorage, availStorage) = getStorageInfo()

        val screenSizeInches = String.format("%.2f", getScreenSizeInInches(metrics))

        return mapOf(
            "Model" to Build.MODEL,
            "Manufacturer" to Build.MANUFACTURER,
            "Board" to Build.BOARD,
            "Screen Size" to "$screenSizeInches inches",
            "Screen Resolution" to "${realMetrics.widthPixels} x ${realMetrics.heightPixels}",
            "Screen Density" to "${metrics.densityDpi} dpi",
            "Total RAM" to formatBytes(totalRam),
            "Available RAM" to formatBytes(availRam),
            "Internal Storage" to formatBytes(totalStorage),
            "Available Storage" to formatBytes(availStorage)
        )
    }

    private fun getTotalRam(context: Context): Long {
    return try {
        val reader = RandomAccessFile("/proc/meminfo", "r")
        val load = reader.readLine()
        reader.close()
        val memKb = load.replace(Regex("\\D+"), "").toLong()
        memKb * 1024
    } catch (e: Exception) {
        e.printStackTrace()
        0L
    }
}


    private fun getAvailableRam(context: Context): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        return memInfo.availMem
    }

    private fun getStorageInfo(): Pair<Long, Long> {
        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val total = stat.blockCountLong * blockSize
        val available = stat.availableBlocksLong * blockSize
        return Pair(total, available)
    }

    private fun formatBytes(bytes: Long): String {
        val df = DecimalFormat("#.##")
        val kb = 1024
        val mb = kb * 1024
        val gb = mb * 1024

        return when {
            bytes >= gb -> df.format(bytes.toFloat() / gb) + " GB"
            bytes >= mb -> df.format(bytes.toFloat() / mb) + " MB"
            bytes >= kb -> df.format(bytes.toFloat() / kb) + " KB"
            else -> "$bytes B"
        }
    }

    private fun getScreenSizeInInches(metrics: DisplayMetrics): Double {
        val widthInches = metrics.widthPixels / metrics.xdpi
        val heightInches = metrics.heightPixels / metrics.ydpi
        return Math.sqrt(widthInches * widthInches + heightInches * heightInches.toDouble())
    }
}

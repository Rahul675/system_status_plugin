package com.tomertech.system_status_plugin

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.*
import android.util.Log
import android.view.WindowManager
import androidx.annotation.NonNull
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.io.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import android.app.ActivityManager
import android.content.res.Resources
import android.os.Build
import android.os.Environment
import android.util.DisplayMetrics
import kotlin.math.sqrt


class SystemStatusPlugin : FlutterPlugin, MethodChannel.MethodCallHandler {
    private lateinit var channel: MethodChannel
    private lateinit var gpuInfoChannel: EventChannel
    private var gpuInfoSink: EventChannel.EventSink? = null

    private var gpuVendor: String? = null
    private var gpuRenderer: String? = null
    private var gpuVersion: String? = null
    private lateinit var context: Context
    private var batteryHandler: Handler? = null
    private var batteryRunnable: Runnable? = null
    private var batteryEventSink: EventChannel.EventSink? = null


    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "system_status_plugin")
        channel.setMethodCallHandler(this)
        context = flutterPluginBinding.applicationContext

        batteryHandler = Handler(Looper.getMainLooper())
        batteryRunnable = object : Runnable {
            override fun run() {
                val batteryInfo = getBatteryInfo(context)
                val eventSink = batteryEventSink
                eventSink?.success(batteryInfo)
                batteryHandler?.postDelayed(this, 3000) // every 3 sec
            }
        }
        batteryHandler?.post(batteryRunnable!!)

        EventChannel(flutterPluginBinding.binaryMessenger, "system_status/uptime").setStreamHandler(
            object : EventChannel.StreamHandler {
                private var handler: Handler? = null
                private var runnable: Runnable? = null

                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    handler = Handler(Looper.getMainLooper())
                    runnable = object : Runnable {
                        override fun run() {
                            val uptime = getFormattedUptime()
                            events?.success(uptime)
                            handler?.postDelayed(this, 1000) // every 1 second
                        }
                    }
                    handler?.post(runnable!!)
                }

                override fun onCancel(arguments: Any?) {
                    handler?.removeCallbacks(runnable!!)
                    handler = null
                    runnable = null
                }
            }
        )


        // ✅ Set up EventChannel for battery info
        EventChannel(flutterPluginBinding.binaryMessenger, "system_status/battery_info").setStreamHandler(
            object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    batteryEventSink = events
                }

                override fun onCancel(arguments: Any?) {
                    batteryEventSink = null
                }
            }
        )


        gpuInfoChannel = EventChannel(flutterPluginBinding.binaryMessenger, "system_status_plugin/gpu_info")
        gpuInfoChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                gpuInfoSink = events
                sendGpuInfo()
            }

            override fun onCancel(arguments: Any?) {
                gpuInfoSink = null
            }
        })
        initGpuInfo(flutterPluginBinding.applicationContext)
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        gpuInfoSink = null
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result) {
        when (call.method) {
            "getCpuInfo" -> result.success(getCpuInfo())
            "getCpuClockSpeeds" -> result.success(getCpuClockSpeeds())
            "getGpuLoad" -> result.success(getGpuLoad())
            "getDeviceDetails" -> result.success(getDeviceDetails())
            "getAvailableRAM" -> result.success(getAvailableRAM())
            "getAvailableStorage" -> result.success(getAvailableStorage())
            "getBatteryInfo" -> result.success(getBatteryInfo(context!!))
            "getSystemInfo" -> result.success(getSystemInfo())
            else -> result.notImplemented()
        }
    }

    private fun getDeviceDetails(): Map<String, Any?> {
        val details = mutableMapOf<String, Any?>()
        details["model"] = Build.MODEL
        details["manufacturer"] = Build.MANUFACTURER
        details["board"] = Build.BOARD
        details["screenSize"] = getNativeScreenSize()
        details["screenResolution"] = getNativeScreenResolution()
        details["screenDisplay"] = getNativeScreenDisplay()
        details["totalRAM"] = getNativeTotalRAM()
        // details["availableRAM"] = getNativeAvailableRAM()
        details["internalStorage"] = getNativeInternalStorage()
        // details["availableStorage"] = getNativeAvailableStorage()
        return details
    }


    private fun getCpuInfo(): Map<String, Any?> {
        val info = mutableMapOf<String, Any?>()
        info["model"] = Build.MODEL
        info["cpuDetails"] = getCpuDetails()
        // info["model"] = readFile("/proc/cpuinfo", "Hardware")
        info["cores"] = Runtime.getRuntime().availableProcessors()
        info["bigLittle"] = getBigLittleDescription()
        info["architecture"] = System.getProperty("os.arch")
        info["revision"] = getRevisionString()
        // info["revision"] = readFile("/proc/cpuinfo", "Revision")
        info["scalingGovernor"] = readGovernor()
        info["gpuVendor"] = gpuVendor
        info["gpuRenderer"] = gpuRenderer
        info["gpuVersion"] = gpuVersion
        return info
    }
    private fun getCpuDetails(): String {
        val cpuPartMap = mapOf(
            "0x0d03" to "Cortex-A53",
            "0x0d05" to "Cortex-A55",
            "0x0d08" to "Cortex-A72",
            "0x0d09" to "Cortex-A73",
            "0x0d0a" to "Cortex-A75",
            "0xd05" to "Cortex-A55",
            "0xd06" to "Cortex-A65",
            "0xd07" to "Cortex-A75",
            "0xd08" to "Cortex-A76",
            "0xd09" to "Cortex-A77",
            "0xd0a" to "Cortex-A78",
            "0xd4a" to "Cortex-X1",
            "0xd41" to "Cortex-A78", // very common
            "0xd44" to "Cortex-X2",
            "0xd46" to "Cortex-A510",
            "0xd47" to "Cortex-A710",
            "0xd48" to "Cortex-X3"
        )

        val parts = mutableSetOf<String>()
        val freqs = mutableSetOf<String>()

        try {
            val lines = File("/proc/cpuinfo").readLines()

            for (line in lines) {
                if (line.startsWith("CPU part", true)) {
                    val rawHex = line.split(":").getOrNull(1)?.trim()?.lowercase()
                    val partName = cpuPartMap[rawHex] ?: rawHex
                    parts.add(partName.toString())
                }
            }

            // Frequencies
            val coreCount = Runtime.getRuntime().availableProcessors()
            for (i in 0 until coreCount) {
                val freqFile = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
                if (freqFile.exists()) {
                    val khz = freqFile.readText().trim().toIntOrNull() ?: continue
                    val ghz = "%.2fGHz".format(khz / 1_000_000f)
                    freqs.add(ghz)
                }
            }

        } catch (e: Exception) {
            return "Unknown"
        }

        return (parts + freqs).joinToString("\n\t\t\t")
    }

    private fun getBatteryInfo(context: Context): Map<String, Any?> {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, intentFilter)

        val info = mutableMapOf<String, Any?>()

        batteryStatus?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val percentage = (level / scale.toFloat()) * 100

            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
            val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
            val technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)

            info["level"] = "${percentage.toInt()}%"
            info["status"] = when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                BatteryManager.BATTERY_STATUS_FULL -> "Full"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                else -> "Unknown"
            }
            info["health"] = when (health) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
                else -> "Unknown"
            }
            info["powerSource"] = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
                BatteryManager.BATTERY_PLUGGED_AC -> "AC Adapter"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                else -> "Battery"
            }
            info["voltage"] = "$voltage"
            info["temperature"] = "%.1f".format(temperature / 10.0)
            info["technology"] = technology ?: "Unknown"
        }

        return info
    }

    @Suppress("DEPRECATION")
    private fun getSystemInfo(): Map<String, String> {
        val info = HashMap<String, String>()

        info["androidVersion"] = Build.VERSION.RELEASE ?: "N/A"
        info["apiLevel"] = Build.VERSION.SDK_INT.toString()
        info["securityPatch"] = Build.VERSION.SECURITY_PATCH ?: "N/A"
        info["bootloader"] = Build.BOOTLOADER ?: "N/A"
        info["buildId"] = Build.ID ?: "N/A"
        info["javaVm"] = System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version")
        info["openGLES"] = (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
            ?.deviceConfigurationInfo?.glEsVersion ?: "N/A"
        info["kernelArch"] = System.getProperty("os.arch") ?: "N/A"
        info["kernelVersion"] = getKernelVersion()
        info["rootAccess"] = if (isDeviceRooted()) "Yes" else "No"
        info["playServices"] = getGooglePlayServicesVersion()
        info["uptime"] = getFormattedUptime()

        return info
    }



    private fun getNativeScreenSize(): String {
        val metrics = Resources.getSystem().displayMetrics
        val widthPixels = metrics.widthPixels
        val heightPixels = metrics.heightPixels
        val xdpi = metrics.xdpi
        val ydpi = metrics.ydpi

        val widthInches = widthPixels / xdpi
        val heightInches = heightPixels / ydpi
        val screenInches = sqrt(widthInches * widthInches + heightInches * heightInches)

        return String.format("%.2f inches", screenInches)
    }

    private fun getNativeScreenResolution(): String {
        val metrics = Resources.getSystem().displayMetrics
        return "${metrics.widthPixels} x ${metrics.heightPixels} px"
    }

    private fun getNativeScreenDisplay(): String {
        val metrics = Resources.getSystem().displayMetrics
        return "${metrics.densityDpi} dpi (${metrics.density})"
    }

    private fun getNativeTotalRAM(): String {
        val reader = RandomAccessFile("/proc/meminfo", "r")
        val load = reader.readLine()
        val memInfo = load.replace(Regex("\\D+"), "")
        val totalMem = memInfo.toLong() * 1024
        reader.close()
        return formatSize(totalMem)
    }

    // private fun getNativeAvailableRAM(): String {
    //     val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    //     val memoryInfo = ActivityManager.MemoryInfo()
    //     activityManager.getMemoryInfo(memoryInfo)
    //     return formatSize(memoryInfo.availMem)
    // }
    private fun getAvailableRAM(): String {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return formatSize(memoryInfo.availMem)
    }

    private fun getNativeInternalStorage(): String {
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalBytes = stat.blockCountLong * stat.blockSizeLong
        return formatSize(totalBytes)
    }

    // private fun getNativeAvailableStorage(): String {
    //     val stat = StatFs(Environment.getDataDirectory().path)
    //     val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
    //     return formatSize(availableBytes)
    // }

    private fun getAvailableStorage(): String {
        val stat = StatFs(Environment.getDataDirectory().path)
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        return formatSize(availableBytes)
    }

    private fun formatSize(size: Long): String {
        val kb = 1024L
        val mb = kb * 1024
        val gb = mb * 1024

        return when {
            size >= gb -> String.format("%.2f GB", size.toFloat() / gb)
            size >= mb -> String.format("%.2f MB", size.toFloat() / mb)
            size >= kb -> String.format("%.2f KB", size.toFloat() / kb)
            else -> "$size B"
        }
    }

    private fun getKernelVersion(): String {
        return try {
            val process = Runtime.getRuntime().exec("uname -r")
            process.inputStream.bufferedReader().readLine() ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun isDeviceRooted(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su"
        )
        return paths.any { File(it).exists() }
    }

    private fun getGooglePlayServicesVersion(): String {
        return try {
            val info = context.packageManager.getPackageInfo("com.google.android.gms", 0)
            info.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Not Installed"
        }
    }

    private fun getFormattedUptime(): String {
        val uptimeMillis = SystemClock.elapsedRealtime()
        val seconds = uptimeMillis / 1000
        val days = seconds / (24 * 3600)
        val hours = (seconds % (24 * 3600)) / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return "$days days, %02d:%02d:%02d".format(hours, minutes, secs)
    }



    private fun getRevisionString(): String {
        var revision = 0
        var variant = 0
        try {
            File("/proc/cpuinfo").useLines { lines ->
                for (line in lines) {
                    when {
                        line.contains("CPU revision", ignoreCase = true) ->
                            revision = line.split(":").getOrNull(1)?.trim()?.toIntOrNull() ?: 0
                        line.contains("CPU variant", ignoreCase = true) ->
                            variant = line.split(":").getOrNull(1)?.trim()?.removePrefix("0x")?.toIntOrNull(16) ?: 0
                    }
                }
            }
        } catch (_: Exception) {}
        return "r${variant}p${revision}"
    }

    private fun getCpuClockSpeeds(): List<String> {
        val speeds = mutableListOf<String>()
        for (i in 0..7) {
            val path = "/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq"
            val freq = try {
                File(path).readText().trim().toInt() / 1000
            } catch (e: Exception) {
                -1
            }
            speeds.add(if (freq > 0) "${freq}MHz" else "N/A")
        }
        return speeds
    }

    private fun getGpuLoad(): String {
        return try {
            val text = File("/sys/class/kgsl/kgsl-3d0/gpubusy").readText().trim()
            val parts = text.split(" ")
            if (parts.size == 2) {
                val busy = parts[0].toFloat()
                val total = parts[1].toFloat()
                if (total > 0) "${(busy / total * 100).toInt()}%" else "0%"
            } else "0%"
        } catch (e: Exception) {
            "0%"
        }
    }

    private fun readFile(filePath: String, key: String): String {
        return try {
            File(filePath).useLines { lines ->
                lines.firstOrNull { it.contains(key) }
                    ?.split(":")?.getOrNull(1)?.trim() ?: "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun readGovernor(): String {
        return try {
            File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor").readText().trim()
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getBigLittleDescription(): String {
        val clusterMap = mutableMapOf<Int, MutableList<Int>>() // freq -> list of core indices

        val cpuCount = Runtime.getRuntime().availableProcessors()

        for (i in 0 until cpuCount) {
            val freqFile = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
            val freq = if (freqFile.exists()) {
                try {
                    freqFile.readText().trim().toInt() / 1000 // MHz
                } catch (e: Exception) {
                    -1
                }
            } else {
                -1
            }

            if (freq != -1) {
                clusterMap.getOrPut(freq) { mutableListOf() }.add(i)
            }
        }

        if (clusterMap.isEmpty()) return "Unknown"

        val clustersDescription = clusterMap.entries.joinToString(", ") { (freq, cores) ->
            val type = when {
                freq >= 2200 -> "Performance"
                freq in 1000..2199 -> "Efficiency"
                else -> "Unknown"
            }
            "${cores.size}x${freq}MHz [$type]"
        }

        return "Yes (${clusterMap.size} clusters: $clustersDescription)"
    }


    private fun initGpuInfo(context: Context) {
        val latch = CountDownLatch(1)

        Handler(Looper.getMainLooper()).post {
            try {
                val glSurfaceView = object : GLSurfaceView(context) {
                    init {
                        setEGLContextClientVersion(2)
                        setRenderer(object : Renderer {
                            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                                try {
                                    gpuVendor = GLES20.glGetString(GLES20.GL_VENDOR) ?: "Unknown"
                                    gpuRenderer = GLES20.glGetString(GLES20.GL_RENDERER) ?: "Unknown"
                                    gpuVersion = GLES20.glGetString(GLES20.GL_VERSION) ?: "Unknown"
                                } catch (_: Exception) {
                                    gpuVendor = "Unknown"
                                    gpuRenderer = "Unknown"
                                    gpuVersion = "Unknown"
                                } finally {
                                    sendGpuInfo()
                                    latch.countDown()
                                }
                            }

                            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {}
                            override fun onDrawFrame(gl: GL10?) {}
                        })
                        renderMode = RENDERMODE_WHEN_DIRTY
                    }
                }

                val layoutParams = WindowManager.LayoutParams(
                    1, 1,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    else
                        WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
                )

                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                windowManager.addView(glSurfaceView, layoutParams)
                glSurfaceView.requestRender()

                Thread {
                    try {
                        latch.await(2, TimeUnit.SECONDS)
                    } finally {
                        Handler(Looper.getMainLooper()).post {
                            try {
                                windowManager.removeView(glSurfaceView)
                            } catch (_: Exception) {}
                        }
                    }
                }.start()
            } catch (_: Exception) {
                // Overlay permission might be missing
            }
        }
    }

    private fun sendGpuInfo() {
        val info = mapOf(
            "gpuVendor" to gpuVendor,
            "gpuRenderer" to gpuRenderer,
            "gpuVersion" to gpuVersion
        )
        gpuInfoSink?.success(info)
    }
}

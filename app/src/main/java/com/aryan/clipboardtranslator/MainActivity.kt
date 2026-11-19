package com.aryan.clipboardtranslator

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 48, 28, 28)
        }

        val title = TextView(this).apply {
            text = "Clipboard Translator"
            textSize = 25f
            setTextColor(0xFF172033.toInt())
        }
        status = TextView(this).apply {
            textSize = 16f
            setPadding(0, 24, 0, 24)
        }

        val enable = Button(this).apply {
            text = "Enable floating button"
            setOnClickListener { enableBubble() }
        }
        val disable = Button(this).apply {
            text = "Stop floating button"
            setOnClickListener {
                stopService(Intent(this@MainActivity, BubbleService::class.java))
                updateStatus()
            }
        }

        root.addView(title)
        root.addView(status)
        root.addView(enable)
        root.addView(disable)
        setContentView(root)
        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) updateStatus()
    }

    private fun enableBubble() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return
        }
        val intent = Intent(this, BubbleService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
        updateStatus()
    }

    private fun updateStatus() {
        if (!::status.isInitialized) return
        status.text = if (Settings.canDrawOverlays(this)) {
            "Overlay permission: granted\nTap Enable to show the floating Hindi button."
        } else {
            "Overlay permission: not granted\nEnable it when Android opens Settings."
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
        }
    }
}

package com.aryan.clipboardtranslator

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView

class BubbleService : Service() {
    private lateinit var windowManager: WindowManager
    private var bubble: View? = null
    private lateinit var params: WindowManager.LayoutParams

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startAsForeground()
        showBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (bubble == null) showBubble()
        return START_STICKY
    }

    private fun startAsForeground() {
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Clipboard Translator is active")
                .setContentText("Tap the floating हिन्दी button after copying text.")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentIntent(openApp)
                .setOngoing(true)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("Clipboard Translator is active")
                .setContentText("Floating translation button is ready.")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setOngoing(true)
                .build()
        }

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Translator service", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun showBubble() {
        if (bubble != null) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val view = TextView(this).apply {
            text = "हि"
            textSize = 19f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF2457C5.toInt())
                setStroke(2, 0xFFFFFFFF.toInt())
            }
            elevation = 12f
            contentDescription = "Translate clipboard to Hindi"
        }

        params = WindowManager.LayoutParams(
            dp(58), dp(58),
            if (Build.VERSION.SDK_INT >= 26)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(18)
            y = dp(220)
        }

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (kotlin.math.abs(dx) > dp(5) || kotlin.math.abs(dy) > dp(5)) moved = true
                    params.x = startX + dx
                    params.y = startY + dy
                    windowManager.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        val intent = Intent(this, ClipboardReaderActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        startActivity(intent)
                    }
                    true
                }
                else -> true
            }
        }

        bubble = view
        windowManager.addView(view, params)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        bubble?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        bubble = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "translator_service"
        private const val NOTIFICATION_ID = 41
    }
}

package com.aryan.clipboardtranslator

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.concurrent.Executors

class ClipboardReaderActivity : Activity() {
    private lateinit var sourceView: TextView
    private lateinit var resultView: TextView
    private lateinit var statusView: TextView
    private var translatedText: String = ""
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 24, 22, 22)
        }
        val title = TextView(this).apply {
            text = "English → Hindi"
            textSize = 23f
            setTextColor(0xFF172033.toInt())
        }
        statusView = TextView(this).apply {
            text = "Reading clipboard..."
            textSize = 14f
            setPadding(0, 12, 0, 12)
        }
        sourceView = TextView(this).apply {
            textSize = 16f
            setTextColor(0xFF333333.toInt())
        }
        resultView = TextView(this).apply {
            textSize = 19f
            setTextColor(0xFF172033.toInt())
            setPadding(0, 18, 0, 18)
        }
        val copyButton = Button(this).apply {
            text = "Copy Hindi translation"
            setOnClickListener {
                if (translatedText.isNotBlank()) {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Hindi translation", translatedText))
                    statusView.text = "Hindi translation copied."
                }
            }
        }
        val closeButton = Button(this).apply {
            text = "Close"
            setOnClickListener { finish() }
        }

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(title)
            addView(statusView)
            addView(TextView(this@ClipboardReaderActivity).apply {
                text = "COPIED TEXT"
                textSize = 12f
                setTextColor(0xFF687386.toInt())
                setPadding(0, 10, 0, 6)
            })
            addView(sourceView)
            addView(TextView(this@ClipboardReaderActivity).apply {
                text = "HINDI TRANSLATION"
                textSize = 12f
                setTextColor(0xFF687386.toInt())
                setPadding(0, 20, 0, 6)
            })
            addView(resultView)
            addView(copyButton)
            addView(closeButton)
        }
        scroll.addView(content)
        root.addView(scroll)
        setContentView(root)

        // This Activity is now foregrounded, so Android permits clipboard access.
        window.decorView.postDelayed({ readAndTranslateClipboard() }, 250)
    }

    private fun readAndTranslateClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = try {
            if (clipboard.hasPrimaryClip()) clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
            else ""
        } catch (e: Exception) {
            statusView.text = "Clipboard access failed: ${e.message}"
            return
        }

        if (text.isBlank()) {
            statusView.text = "Clipboard is empty. Copy some text and tap the floating button again."
            return
        }

        sourceView.text = text
        statusView.text = "Translating..."
        executor.execute {
            try {
                val hindi = translateWithGoogle(text)
                runOnUiThread {
                    translatedText = hindi
                    resultView.text = hindi
                    statusView.text = "Translation complete."
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusView.text = "Translation failed: ${e.message}"
                    resultView.text = "Check your internet connection and try again."
                }
            }
        }
    }

    // Uses Google's unofficial web translation endpoint. It does not require an API key,
    // but Google may throttle requests; this is not a supported production API.
    private fun translateWithGoogle(input: String): String {
        require(input.length <= 4500) { "Text is too long. Please copy a shorter passage." }

        val encoded = URLEncoder.encode(input, "UTF-8")
        val endpoint = "https://translate.googleapis.com/translate_a/single" +
            "?client=gtx&sl=en&tl=hi&dt=t&q=$encoded"

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 20000
            setRequestProperty("User-Agent", "Mozilla/5.0")
        }

        try {
            val code = connection.responseCode
            if (code !in 200..299) throw Exception("Google endpoint returned HTTP $code")
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JSONArray(body)
            val chunks = root.getJSONArray(0)
            val output = StringBuilder()
            for (i in 0 until chunks.length()) {
                val chunk = chunks.optJSONArray(i) ?: continue
                val part = chunk.optString(0)
                if (part != "null") output.append(part)
            }
            return output.toString().ifBlank { throw Exception("Empty translation received") }
        } finally {
            connection.disconnect()
        }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}

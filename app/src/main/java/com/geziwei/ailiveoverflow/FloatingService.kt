package com.geziwei.ailiveoverflow

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.NotificationCompat

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var petWebView: WebView
    private lateinit var windowParams: WindowManager.LayoutParams

    // 拖拽状态
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var dragStartParamX = 0
    private var dragStartParamY = 0
    private var isDragging = false
    private val DRAG_THRESHOLD = 8f

    companion object {
        const val CHANNEL_ID = "ghost_channel"
        const val NOTIF_ID = 1
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(
            NOTIF_ID,
            buildNotification(),
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
        setupFloatingView()
    }

    private fun setupFloatingView() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_ghost, null)
        petWebView = floatingView.findViewById(R.id.petWebView)

        // WebView 配置
        petWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }
        petWebView.setBackgroundColor(0x00000000)
        petWebView.webViewClient = WebViewClient()
        petWebView.loadUrl("file:///android_asset/pet.html")

        // 悬浮窗参数
        // dp 转 px：200dp × 220dp，与 pet.html 的 stage 尺寸对应
        val density = resources.displayMetrics.density
        val wPx = (80 * density).toInt()
        val hPx = (80 * density).toInt()
        windowParams = WindowManager.LayoutParams(
            wPx,
            hPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 20
            y = 200
        }

        // 拖拽挂在 WebView 上：WebView 自己收事件，超过阈值时移动悬浮窗，
        // 未超阈值时把事件透传给 WebView 内部 JS 处理手势。
        petWebView.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dragStartX = event.rawX
                    dragStartY = event.rawY
                    dragStartParamX = windowParams.x
                    dragStartParamY = windowParams.y
                    isDragging = false
                    v.onTouchEvent(event) // 传给 WebView，让 JS 也收到 DOWN
                    true // 必须消费 DOWN，否则收不到后续 MOVE
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - dragStartX
                    val dy = event.rawY - dragStartY
                    if (!isDragging && (Math.abs(dx) > DRAG_THRESHOLD || Math.abs(dy) > DRAG_THRESHOLD)) {
                        isDragging = true
                    }
                    if (isDragging) {
                        windowParams.x = (dragStartParamX - dx).toInt()
                        windowParams.y = (dragStartParamY + dy).toInt()
                        windowManager.updateViewLayout(floatingView, windowParams)
                        true
                    } else {
                        v.onTouchEvent(event)
                        true
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!isDragging) {
                        v.onTouchEvent(event) // 没拖拽，透传 UP 给 WebView JS
                    }
                    isDragging = false
                    true
                }
                else -> v.onTouchEvent(event)
            }
        }

        windowManager.addView(floatingView, windowParams)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::petWebView.isInitialized) petWebView.destroy()
        if (::floatingView.isInitialized) windowManager.removeView(floatingView)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "醇·桌宠", NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("醇在陪着你")
            .setContentText("单击互动 · 双击打招呼/比耶 · 长按生气")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
}

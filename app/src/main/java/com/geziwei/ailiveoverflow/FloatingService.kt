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
        windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 20
            y = 200
        }

        // 拖拽手势：在 floatingView 层处理，不拦截传给 WebView 的点击事件
        floatingView.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dragStartX = event.rawX
                    dragStartY = event.rawY
                    dragStartParamX = windowParams.x
                    dragStartParamY = windowParams.y
                    isDragging = false
                    false // 不消费，先传给 WebView
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
                        true // 拖拽时消费事件
                    } else {
                        false
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val wasDragging = isDragging
                    isDragging = false
                    wasDragging // 拖拽结束消费事件，点击则不消费让 WebView 处理
                }
                else -> false
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

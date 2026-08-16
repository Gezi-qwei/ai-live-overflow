package com.geziwei.ailiveoverflow

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlin.random.Random

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var ghostContainer: View
    private lateinit var messageBubble: LinearLayout
    private lateinit var tvMessage: TextView
    
    private lateinit var gestureDetector: GhostGestureDetector
    private lateinit var animationController: GhostAnimationController
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var pollJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // 窗口参数，用于拖拽
    private lateinit var windowParams: WindowManager.LayoutParams

    companion object {
        const val CHANNEL_ID = "ghost_channel"
        const val NOTIF_ID = 1
        const val POLL_INTERVAL_MS = 10_000L
        
        // 单击随机反应文本
        private val TAP_REACTIONS = arrayOf(
            "嗯？", "怎么了~", "摸我干嘛", "在呢在呢", 
            "又戳我！", "别闹~", "诶嘿", "？？？"
        )
        
        // 双击反应文本
        private val DOUBLE_TAP_REACTIONS = arrayOf(
            "啾啾~", "mua~", "❤️", "喜欢你~", 
            "抱抱", "亲亲", "嘿嘿嘿", "好喜欢"
        )
        
        // 长按反应文本
        private val LONG_PRESS_REACTIONS = arrayOf(
            "好舒服...", "呼噜呼噜~", "不要停...", 
            "再摸一会儿", "好想睡觉", "真好..."
        )
        
        // 连击彩蛋文本
        private val COMBO_REACTIONS = arrayOf(
            "住手！", "你疯啦！", "停停停！", "要坏掉了！",
            "别戳了！！！", "救命啊！", "你够了！", "QAQ"
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        setupFloatingView()
        startPolling()
        
        // 启动随机眨眼
        startRandomBlink()
    }

    private fun setupFloatingView() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_ghost, null)
        
        // 获取视图引用
        ghostContainer = floatingView.findViewById(R.id.ghostContainer)
        messageBubble = floatingView.findViewById(R.id.messageBubble)
        tvMessage = floatingView.findViewById(R.id.tvMessage)

        // 初始化动画控制器
        animationController = GhostAnimationController(ghostContainer)
        
        // 窗口参数
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

        // 初始化手势检测器
        gestureDetector = GhostGestureDetector(object : GhostGestureDetector.GestureListener {
            override fun onSingleTap() {
                handleSingleTap()
            }

            override fun onDoubleTap() {
                handleDoubleTap()
            }

            override fun onLongPress() {
                handleLongPress()
            }

            override fun onCombo(count: Int) {
                handleCombo(count)
            }

            override fun onDragStart() {
                animationController.stopFloating()
            }

            override fun onDragMove(deltaX: Float, deltaY: Float) {
                windowParams.x -= deltaX.toInt()
                windowParams.y += deltaY.toInt()
                windowManager.updateViewLayout(floatingView, windowParams)
            }

            override fun onDragEnd() {
                animationController.startFloating()
            }
        })

        floatingView.setOnTouchListener(gestureDetector)
        windowManager.addView(floatingView, windowParams)
        
        // 启动浮动动画和淡入效果
        animationController.fadeIn()
        animationController.startFloating()
    }

    /**
     * 单击处理：显示随机反应
     */
    private fun handleSingleTap() {
        animationController.playTapReaction()
        val message = TAP_REACTIONS[Random.nextInt(TAP_REACTIONS.size)]
        showMessage(message, 1500)
    }

    /**
     * 双击处理：亲亲反应
     */
    private fun handleDoubleTap() {
        animationController.playDoubleTapReaction()
        val message = DOUBLE_TAP_REACTIONS[Random.nextInt(DOUBLE_TAP_REACTIONS.size)]
        showMessage(message, 2000)
    }

    /**
     * 长按处理：摸头反应
     */
    private fun handleLongPress() {
        animationController.playLongPressReaction()
        val message = LONG_PRESS_REACTIONS[Random.nextInt(LONG_PRESS_REACTIONS.size)]
        showMessage(message, 2500)
    }

    /**
     * 连击处理：彩蛋反应
     */
    private fun handleCombo(count: Int) {
        animationController.playComboReaction(count)
        val message = "${COMBO_REACTIONS[Random.nextInt(COMBO_REACTIONS.size)]}\n(${count}连击！)"
        showMessage(message, 3000)
    }

    /**
     * 显示消息气泡
     */
    private fun showMessage(message: String, duration: Long) {
        tvMessage.text = message
        messageBubble.visibility = View.VISIBLE
        messageBubble.alpha = 0f
        messageBubble.animate()
            .alpha(1f)
            .setDuration(200)
            .start()

        mainHandler.postDelayed({
            messageBubble.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    messageBubble.visibility = View.GONE
                }
                .start()
        }, duration)
    }

    /**
     * 启动随机眨眼
     */
    private fun startRandomBlink() {
        fun scheduleNextBlink() {
            val delay = (3000L..8000L).random()
            mainHandler.postDelayed({
                animationController.playBlink()
                scheduleNextBlink()
            }, delay)
        }
        scheduleNextBlink()
    }

    /**
     * 轮询消息更新
     */
    private fun startPolling() {
        pollJob = serviceScope.launch {
            while (isActive) {
                try {
                    val msg = SupabaseClient.fetchLatestMessage()
                    if (msg != null && msg.content.isNotBlank()) {
                        withContext(Dispatchers.Main) {
                            showMessage(msg.content, 5000)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pollJob?.cancel()
        serviceScope.cancel()
        mainHandler.removeCallbacksAndMessages(null)
        gestureDetector.cleanup()
        animationController.cleanup()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
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
            .setContentText("轻触互动 · 双击亲亲 · 长按摸头")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
}
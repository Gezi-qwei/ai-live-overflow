package com.geziwei.ailiveoverflow

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View

/**
 * 幽灵手势检测器
 * 支持：单击、双击、长按、拖拽、连击
 */
class GhostGestureDetector(
    private val listener: GestureListener
) : View.OnTouchListener {

    companion object {
        private const val LONG_PRESS_TIMEOUT = 500L
        private const val DOUBLE_TAP_TIMEOUT = 300L
        private const val TAP_SLOP = 10f
        private const val COMBO_WINDOW = 1000L
        private const val COMBO_THRESHOLD = 5
    }

    interface GestureListener {
        fun onSingleTap()
        fun onDoubleTap()
        fun onLongPress()
        fun onCombo(count: Int)
        fun onDragStart()
        fun onDragMove(deltaX: Float, deltaY: Float)
        fun onDragEnd()
    }

    private val handler = Handler(Looper.getMainLooper())
    private var downTime = 0L
    private var downX = 0f
    private var downY = 0f
    private var lastTapTime = 0L
    private var tapCount = 0
    private var isDragging = false
    private var longPressTriggered = false
    private var lastMoveX = 0f
    private var lastMoveY = 0f

    // 连击计数
    private var comboCount = 0
    private var lastComboTime = 0L
    private val comboResetRunnable = Runnable {
        if (comboCount >= COMBO_THRESHOLD) {
            listener.onCombo(comboCount)
        }
        comboCount = 0
    }

    // 长按检测
    private val longPressRunnable = Runnable {
        if (!isDragging) {
            longPressTriggered = true
            listener.onLongPress()
        }
    }

    // 双击检测
    private val doubleTapRunnable = Runnable {
        if (tapCount == 1) {
            listener.onSingleTap()
        }
        tapCount = 0
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                downX = event.rawX
                downY = event.rawY
                lastMoveX = event.rawX
                lastMoveY = event.rawY
                isDragging = false
                longPressTriggered = false

                // 启动长按检测
                handler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT)

                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - downX
                val deltaY = event.rawY - downY
                val distance = Math.sqrt((deltaX * deltaX + deltaY * deltaY).toDouble()).toFloat()

                if (!isDragging && distance > TAP_SLOP) {
                    isDragging = true
                    handler.removeCallbacks(longPressRunnable)
                    listener.onDragStart()
                }

                if (isDragging) {
                    val moveDeltaX = event.rawX - lastMoveX
                    val moveDeltaY = event.rawY - lastMoveY
                    listener.onDragMove(moveDeltaX, moveDeltaY)
                    lastMoveX = event.rawX
                    lastMoveY = event.rawY
                }

                return true
            }

            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(longPressRunnable)

                val upTime = System.currentTimeMillis()
                val deltaX = event.rawX - downX
                val deltaY = event.rawY - downY
                val distance = Math.sqrt((deltaX * deltaX + deltaY * deltaY).toDouble()).toFloat()

                if (isDragging) {
                    listener.onDragEnd()
                    isDragging = false
                } else if (!longPressTriggered && distance < TAP_SLOP) {
                    // 这是一次点击
                    val timeSinceLastTap = upTime - lastTapTime

                    if (timeSinceLastTap < DOUBLE_TAP_TIMEOUT) {
                        // 双击
                        handler.removeCallbacks(doubleTapRunnable)
                        tapCount++
                        if (tapCount == 2) {
                            listener.onDoubleTap()
                            tapCount = 0
                        }
                    } else {
                        // 单击（延迟确认，等待可能的双击）
                        tapCount = 1
                        handler.postDelayed(doubleTapRunnable, DOUBLE_TAP_TIMEOUT)
                    }

                    lastTapTime = upTime

                    // 连击检测
                    if (upTime - lastComboTime < COMBO_WINDOW) {
                        comboCount++
                        handler.removeCallbacks(comboResetRunnable)
                        handler.postDelayed(comboResetRunnable, COMBO_WINDOW)
                    } else {
                        comboCount = 1
                        handler.postDelayed(comboResetRunnable, COMBO_WINDOW)
                    }
                    lastComboTime = upTime
                }

                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)
                handler.removeCallbacks(doubleTapRunnable)
                if (isDragging) {
                    listener.onDragEnd()
                    isDragging = false
                }
                return true
            }
        }

        return false
    }

    fun cleanup() {
        handler.removeCallbacks(longPressRunnable)
        handler.removeCallbacks(doubleTapRunnable)
        handler.removeCallbacks(comboResetRunnable)
    }
}

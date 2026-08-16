package com.geziwei.ailiveoverflow

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

/**
 * 幽灵动画控制器
 * 负责浮动、反馈动画、眨眼等
 */
class GhostAnimationController(
    private val ghostView: View
) {

    private var floatAnimator: ValueAnimator? = null
    private var isFloating = true

    /**
     * 启动浮动动画（上下缓慢浮动）
     */
    fun startFloating() {
        if (floatAnimator != null) return

        isFloating = true
        val initialY = ghostView.translationY

        floatAnimator = ValueAnimator.ofFloat(-15f, 15f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener { animation ->
                if (isFloating) {
                    ghostView.translationY = initialY + (animation.animatedValue as Float)
                }
            }

            start()
        }
    }

    /**
     * 停止浮动动画
     */
    fun stopFloating() {
        isFloating = false
        floatAnimator?.cancel()
        floatAnimator = null
        ghostView.animate()
            .translationY(0f)
            .setDuration(200)
            .start()
    }

    /**
     * 单击反应：轻微跳跃
     */
    fun playTapReaction() {
        val originalY = ghostView.translationY
        ghostView.animate()
            .translationY(originalY - 30f)
            .setDuration(150)
            .withEndAction {
                ghostView.animate()
                    .translationY(originalY)
                    .setDuration(150)
                    .start()
            }
            .start()
    }

    /**
     * 双击反应：爱心跳动（缩放）
     */
    fun playDoubleTapReaction() {
        val scaleAnim = AnimatorSet().apply {
            playSequentially(
                ObjectAnimator.ofFloat(ghostView, "scaleX", 1f, 1.2f).setDuration(100),
                ObjectAnimator.ofFloat(ghostView, "scaleX", 1.2f, 1f).setDuration(100),
                ObjectAnimator.ofFloat(ghostView, "scaleY", 1f, 1.2f).setDuration(100),
                ObjectAnimator.ofFloat(ghostView, "scaleY", 1.2f, 1f).setDuration(100)
            )
        }
        scaleAnim.start()
    }

    /**
     * 长按反应：满足地摇晃
     */
    fun playLongPressReaction() {
        val rotateAnim = AnimatorSet().apply {
            playSequentially(
                ObjectAnimator.ofFloat(ghostView, "rotation", 0f, -10f).setDuration(100),
                ObjectAnimator.ofFloat(ghostView, "rotation", -10f, 10f).setDuration(200),
                ObjectAnimator.ofFloat(ghostView, "rotation", 10f, -10f).setDuration(200),
                ObjectAnimator.ofFloat(ghostView, "rotation", -10f, 0f).setDuration(100)
            )
        }
        rotateAnim.start()
    }

    /**
     * 连击反应：疯狂旋转
     */
    fun playComboReaction(count: Int) {
        val rotations = count.toFloat() * 360f
        ObjectAnimator.ofFloat(ghostView, "rotation", 0f, rotations).apply {
            duration = 800
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    /**
     * 眨眼动画（透明度闪烁）
     */
    fun playBlink() {
        ghostView.animate()
            .alpha(0.5f)
            .setDuration(80)
            .withEndAction {
                ghostView.animate()
                    .alpha(1f)
                    .setDuration(80)
                    .start()
            }
            .start()
    }

    /**
     * 抖动动画（警告/不满）
     */
    fun playShake() {
        val originalX = ghostView.translationX
        AnimatorSet().apply {
            playSequentially(
                ObjectAnimator.ofFloat(ghostView, "translationX", originalX, originalX - 15f).setDuration(50),
                ObjectAnimator.ofFloat(ghostView, "translationX", originalX - 15f, originalX + 15f).setDuration(100),
                ObjectAnimator.ofFloat(ghostView, "translationX", originalX + 15f, originalX - 15f).setDuration(100),
                ObjectAnimator.ofFloat(ghostView, "translationX", originalX - 15f, originalX).setDuration(50)
            )
            start()
        }
    }

    /**
     * 淡入动画
     */
    fun fadeIn(duration: Long = 300) {
        ghostView.alpha = 0f
        ghostView.animate()
            .alpha(1f)
            .setDuration(duration)
            .start()
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        floatAnimator?.cancel()
        floatAnimator = null
        ghostView.clearAnimation()
    }
}
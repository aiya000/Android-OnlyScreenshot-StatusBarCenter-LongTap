package io.github.aiya000.onlyscreenshot

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.abs

/** The zone when it is being shown, translucent enough to aim with but not to hide anything. */
private const val HighlightColor = 0x555C6BC0

/**
 * The transparent strip that sits over the status bar and watches the finger.
 *
 * It has to consume the touches it sees -- a window either takes a touch or does not get
 * it at all -- so the gestures it swallows are handed back through [onSwipeDown], which
 * the service turns into the notification panel the user was reaching for.
 */
@SuppressLint("ViewConstructor")
class HotZoneView(
    context: Context,
    private val onLongPress: () -> Unit,
    private val onSwipeDown: () -> Unit,
) : View(context) {

    private var longPressMillis = HotZoneSettings().longPressMillis.toLong()
    private var swipeDownEnabled = true

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var downX = 0f
    private var downY = 0f

    /** Set once the gesture has been decided, so that a single touch fires at most one action. */
    private var decided = false

    private val longPress = Runnable {
        decided = true
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        onLongPress()
    }

    fun applySettings(settings: HotZoneSettings) {
        longPressMillis = settings.longPressMillis.toLong()
        swipeDownEnabled = settings.swipeDownOpensShade
        setBackgroundColor(if (settings.showZone) HighlightColor else Color.TRANSPARENT)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                decided = false
                postDelayed(longPress, longPressMillis)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (decided) return true
                val dx = event.x - downX
                val dy = event.y - downY
                if (dy > touchSlop && dy > abs(dx)) {
                    // A pull towards the notification panel. It can no longer reach the
                    // status bar, so open the panel outright.
                    removeCallbacks(longPress)
                    decided = true
                    if (swipeDownEnabled) onSwipeDown()
                } else if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                    // Moved off in some other direction; this is not a long press any more.
                    removeCallbacks(longPress)
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(longPress)
                return true
            }
        }
        return true
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(longPress)
        super.onDetachedFromWindow()
    }
}

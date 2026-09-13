package io.github.aiya000.onlyscreenshot

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import java.util.concurrent.Executors

/**
 * The app itself: a transparent strip over the middle of the status bar, and a screenshot
 * whenever it is long pressed.
 *
 * It is an accessibility service for two reasons that no other kind of component can
 * offer. `TYPE_ACCESSIBILITY_OVERLAY` is the only window type that is laid out *above*
 * the status bar -- everything an ordinary app can add with `SYSTEM_ALERT_WINDOW` is put
 * below it since Android 8, where the status bar would go on taking every touch. And
 * [takeScreenshot] needs no consent dialog, where `MediaProjection` puts one in front of
 * the user every single time from Android 14 on.
 *
 * Nothing here reads the screen: the service declares no event types and no
 * `canRetrieveWindowContent`.
 */
class ShotService : AccessibilityService() {

    private lateinit var windowManager: WindowManager

    private var hotZone: HotZoneView? = null
    private var settings = HotZoneSettings()

    private val mainHandler = Handler(Looper.getMainLooper())

    /** Screenshots are taken onto this thread, so that the PNG is written off the main one. */
    private val screenshotExecutor = Executors.newSingleThreadExecutor()

    private val settingsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        // Held as a field: the preferences manager keeps only a weak reference to it.
        mainHandler.post(::applySettings)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WindowManager::class.java)
        settings = readSettings()
        addHotZone()
        settingsPreferences().registerOnSharedPreferenceChangeListener(settingsListener)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // A rotation or a fold changes the screen width the zone is measured against.
        updateHotZoneLayout()
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        removeHotZone()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        settingsPreferences().unregisterOnSharedPreferenceChangeListener(settingsListener)
        removeHotZone()
        screenshotExecutor.shutdown()
        super.onDestroy()
    }

    /** No event types are declared, so this is never called. */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    // ---- the hot zone ----

    private fun addHotZone() {
        removeHotZone()
        val view = HotZoneView(
            context = this,
            onLongPress = ::capture,
            onSwipeDown = { performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) },
        )
        view.applySettings(settings)
        windowManager.addView(view, hotZoneLayoutParams())
        hotZone = view
    }

    private fun removeHotZone() {
        hotZone?.let { runCatching { windowManager.removeView(it) } }
        hotZone = null
    }

    private fun applySettings() {
        settings = readSettings()
        hotZone?.applySettings(settings)
        updateHotZoneLayout()
    }

    private fun updateHotZoneLayout() {
        val view = hotZone ?: return
        runCatching { windowManager.updateViewLayout(view, hotZoneLayoutParams()) }
    }

    private fun hotZoneLayoutParams(): WindowManager.LayoutParams {
        val screenWidth = screenWidth()
        val statusBarHeight = statusBarHeight()
        return WindowManager.LayoutParams(
            (screenWidth * settings.widthPercent / 100).coerceAtLeast(1),
            (statusBarHeight * settings.heightPercent / 100).coerceAtLeast(1),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                // Only the strip itself takes touches; everything around it is untouched.
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = screenWidth * settings.offsetPercent / 100
            y = statusBarHeight * settings.topOffsetPercent / 100
            // The middle of the status bar is exactly where a cutout tends to be, so the
            // window has to be allowed into it.
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            title = "OnlyScreenshot hot zone"
        }
    }

    private fun screenWidth(): Int =
        runCatching { windowManager.maximumWindowMetrics.bounds.width() }
            .getOrDefault(resources.displayMetrics.widthPixels)

    /**
     * The status bar height as the framework itself measures it. A service has no window
     * to ask for insets, so this reads the platform dimension the status bar is laid out
     * with, and falls back to the 24dp it has been for years.
     */
    private fun statusBarHeight(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (id > 0) return resources.getDimensionPixelSize(id)
        return (24 * resources.displayMetrics.density).toInt()
    }

    // ---- the screenshot ----

    private fun capture() {
        when (settings.captureMode) {
            CaptureMode.System -> performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            CaptureMode.Silent -> captureSilently()
        }
    }

    private fun captureSilently() {
        takeScreenshot(
            Display.DEFAULT_DISPLAY,
            screenshotExecutor,
            object : TakeScreenshotCallback {
                override fun onSuccess(screenshot: ScreenshotResult) {
                    // The buffer is hardware backed and has to be released; PNG encoding
                    // needs a software bitmap, hence the copy.
                    val bitmap = screenshot.hardwareBuffer.use { buffer ->
                        Bitmap.wrapHardwareBuffer(buffer, screenshot.colorSpace)
                            ?.copy(Bitmap.Config.ARGB_8888, false)
                    }
                    if (bitmap == null) {
                        toast(getString(R.string.save_failed))
                        return
                    }
                    val name = saveScreenshot(bitmap)
                    bitmap.recycle()
                    toast(
                        if (name == null) getString(R.string.save_failed)
                        else getString(R.string.saved_to, name),
                    )
                }

                override fun onFailure(errorCode: Int) {
                    toast(getString(R.string.capture_failed, errorCode))
                }
            },
        )
    }

    private fun toast(message: String) {
        mainHandler.post { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
    }
}

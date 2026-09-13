package io.github.aiya000.onlyscreenshot

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.core.content.edit

private const val PreferencesName = "only_screenshot"

private const val WidthPercentKey = "zone_width_percent"
private const val HeightPercentKey = "zone_height_percent"
private const val OffsetPercentKey = "zone_offset_percent"
private const val TopOffsetPercentKey = "zone_top_offset_percent"
private const val LongPressMillisKey = "long_press_millis"
private const val ShowZoneKey = "show_zone"
private const val SwipeDownOpensShadeKey = "swipe_down_opens_shade"
private const val CaptureModeKey = "capture_mode"

/** How the screenshot itself is taken once the long press has been recognised. */
enum class CaptureMode {
    /** The system screenshot, with its flash and its preview. Saved wherever the device saves screenshots. */
    System,

    /** Taken by the service and written to Pictures/Screenshots by this app, with no preview. */
    Silent,
}

/**
 * Everything about the hot zone is adjustable, because where the middle of the status bar
 * is depends on the device -- a cutout, a punch hole, a curved corner all move it.
 *
 * The width and the horizontal offset are percentages of the screen width; the height
 * and the top offset are percentages of the status bar height, so that a height of 100
 * with a top offset of 0 is exactly the status bar, and a top offset of 100 drops the
 * zone to just below it.
 */
data class HotZoneSettings(
    val widthPercent: Int = 30,
    val heightPercent: Int = 100,
    val offsetPercent: Int = 0,
    val topOffsetPercent: Int = 0,
    val longPressMillis: Int = 500,
    val showZone: Boolean = false,
    val swipeDownOpensShade: Boolean = true,
    val captureMode: CaptureMode = CaptureMode.System,
)

val WidthPercentRange = 5..100
val HeightPercentRange = 50..400
val OffsetPercentRange = -50..50
val TopOffsetPercentRange = 0..300
val LongPressMillisRange = 200..1500

fun Context.readSettings(): HotZoneSettings {
    val preferences = settingsPreferences()
    val defaults = HotZoneSettings()
    return HotZoneSettings(
        widthPercent = preferences.getInt(WidthPercentKey, defaults.widthPercent),
        heightPercent = preferences.getInt(HeightPercentKey, defaults.heightPercent),
        offsetPercent = preferences.getInt(OffsetPercentKey, defaults.offsetPercent),
        topOffsetPercent = preferences.getInt(TopOffsetPercentKey, defaults.topOffsetPercent),
        longPressMillis = preferences.getInt(LongPressMillisKey, defaults.longPressMillis),
        showZone = preferences.getBoolean(ShowZoneKey, defaults.showZone),
        swipeDownOpensShade = preferences.getBoolean(SwipeDownOpensShadeKey, defaults.swipeDownOpensShade),
        captureMode = runCatching {
            CaptureMode.valueOf(preferences.getString(CaptureModeKey, null) ?: defaults.captureMode.name)
        }.getOrDefault(defaults.captureMode),
    )
}

fun Context.writeSettings(settings: HotZoneSettings) {
    settingsPreferences().edit {
        putInt(WidthPercentKey, settings.widthPercent)
        putInt(HeightPercentKey, settings.heightPercent)
        putInt(OffsetPercentKey, settings.offsetPercent)
        putInt(TopOffsetPercentKey, settings.topOffsetPercent)
        putInt(LongPressMillisKey, settings.longPressMillis)
        putBoolean(ShowZoneKey, settings.showZone)
        putBoolean(SwipeDownOpensShadeKey, settings.swipeDownOpensShade)
        putString(CaptureModeKey, settings.captureMode.name)
    }
}

/**
 * The same file the service watches, so that a slider moved on the settings screen moves
 * the zone on screen at once.
 */
fun Context.settingsPreferences() = getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)

/**
 * Whether [ShotService] is switched on in the system settings. Nothing this app does
 * works until it is, and only the user can do it.
 */
fun Context.isShotServiceEnabled(): Boolean {
    val expected = ComponentName(this, ShotService::class.java)
    val enabled = Settings.Secure.getString(
        contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ).orEmpty()
    return enabled.split(':')
        .mapNotNull(ComponentName::unflattenFromString)
        .any { it == expected }
}

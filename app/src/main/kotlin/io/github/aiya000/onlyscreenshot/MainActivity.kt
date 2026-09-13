package io.github.aiya000.onlyscreenshot

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The settings screen, and the way into the system settings where the service is switched
 * on. The app does nothing else -- the work all happens in [ShotService].
 */
class MainActivity : ComponentActivity() {

    private var serviceEnabled by mutableStateOf(false)
    private var settings by mutableStateOf(HotZoneSettings())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = readSettings()

        setContent {
            MaterialTheme(colorScheme = OnlyScreenshotColorScheme) {
                SettingsScreen(
                    serviceEnabled = serviceEnabled,
                    settings = settings,
                    onSettingsChanged = {
                        settings = it
                        // The service watches the same file and moves the zone at once.
                        writeSettings(it)
                    },
                    onOpenAccessibilitySettings = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Coming back from the system settings is the only way this can have changed.
        serviceEnabled = isShotServiceEnabled()
    }
}

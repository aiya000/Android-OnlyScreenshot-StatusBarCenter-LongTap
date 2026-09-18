package io.github.aiya000.onlyscreenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    serviceEnabled: Boolean,
    settings: HotZoneSettings,
    onSettingsChanged: (HotZoneSettings) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ServiceCard(
                serviceEnabled = serviceEnabled,
                onOpenAccessibilitySettings = onOpenAccessibilitySettings,
            )

            Section(stringResource(R.string.section_zone)) {
                PercentSlider(
                    label = stringResource(R.string.zone_width),
                    value = settings.widthPercent,
                    range = WidthPercentRange,
                    onValueChange = { onSettingsChanged(settings.copy(widthPercent = it)) },
                )
                PercentSlider(
                    label = stringResource(R.string.zone_height),
                    value = settings.heightPercent,
                    range = HeightPercentRange,
                    onValueChange = { onSettingsChanged(settings.copy(heightPercent = it)) },
                )
                PercentSlider(
                    label = stringResource(R.string.zone_offset),
                    value = settings.offsetPercent,
                    range = OffsetPercentRange,
                    onValueChange = { onSettingsChanged(settings.copy(offsetPercent = it)) },
                )
                PercentSlider(
                    label = stringResource(R.string.zone_top_offset),
                    value = settings.topOffsetPercent,
                    range = TopOffsetPercentRange,
                    onValueChange = { onSettingsChanged(settings.copy(topOffsetPercent = it)) },
                )
                IntSlider(
                    label = stringResource(R.string.long_press_duration),
                    valueText = stringResource(R.string.millis_value, settings.longPressMillis),
                    value = settings.longPressMillis,
                    range = LongPressMillisRange,
                    step = 50,
                    onValueChange = { onSettingsChanged(settings.copy(longPressMillis = it)) },
                )
                IntSlider(
                    label = stringResource(R.string.move_tolerance),
                    valueText = stringResource(R.string.dp_value, settings.moveToleranceDp),
                    value = settings.moveToleranceDp,
                    range = MoveToleranceDpRange,
                    step = 1,
                    onValueChange = { onSettingsChanged(settings.copy(moveToleranceDp = it)) },
                )
            }

            Section(stringResource(R.string.section_behavior)) {
                SwitchRow(
                    label = stringResource(R.string.show_zone),
                    description = stringResource(R.string.show_zone_description),
                    checked = settings.showZone,
                    onCheckedChange = { onSettingsChanged(settings.copy(showZone = it)) },
                )
                SwitchRow(
                    label = stringResource(R.string.swipe_down_opens_shade),
                    description = stringResource(R.string.swipe_down_opens_shade_description),
                    checked = settings.swipeDownOpensShade,
                    onCheckedChange = { onSettingsChanged(settings.copy(swipeDownOpensShade = it)) },
                )

                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.capture_mode),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Column(Modifier.selectableGroup()) {
                    CaptureModeRow(
                        label = stringResource(R.string.capture_mode_system),
                        selected = settings.captureMode == CaptureMode.System,
                        onSelect = { onSettingsChanged(settings.copy(captureMode = CaptureMode.System)) },
                    )
                    CaptureModeRow(
                        label = stringResource(R.string.capture_mode_silent),
                        selected = settings.captureMode == CaptureMode.Silent,
                        onSelect = { onSettingsChanged(settings.copy(captureMode = CaptureMode.Silent)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceCard(serviceEnabled: Boolean, onOpenAccessibilitySettings: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(
                    if (serviceEnabled) R.string.status_enabled else R.string.status_disabled,
                ),
                color = if (serviceEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.error
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = onOpenAccessibilitySettings, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.open_accessibility_settings))
            }
            Text(
                text = stringResource(R.string.sideload_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        content()
    }
}

@Composable
private fun PercentSlider(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
) {
    IntSlider(
        label = label,
        valueText = stringResource(R.string.percent_value, value),
        value = value,
        range = range,
        step = 1,
        onValueChange = onValueChange,
    )
}

@Composable
private fun IntSlider(
    label: String,
    valueText: String,
    value: Int,
    range: IntRange,
    step: Int,
    onValueChange: (Int) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(valueText, style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = {
                val stepped = (it / step).roundToInt() * step
                onValueChange(stepped.coerceIn(range.first, range.last))
            },
            valueRange = range.first.toFloat()..range.last.toFloat(),
        )
    }
}

@Composable
private fun SwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun CaptureModeRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Start,
        )
    }
}

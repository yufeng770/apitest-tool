package com.example.yuapitest

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: MotionSettings,
    onSettingsChange: (MotionSettings) -> Unit,
    onSettingsSettled: (MotionSettings) -> Unit
) {
    var previewAtEnd by remember { mutableStateOf(false) }
    val previewProgress by animateFloatAsState(
        targetValue = if (previewAtEnd) 1f else 0f,
        animationSpec = settings.springSpec(),
        label = "motion-preview"
    )

    LaunchedEffect(settings) {
        previewAtEnd = !previewAtEnd
        delay(700)
        previewAtEnd = !previewAtEnd
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = {
                    val defaults = MotionSettings()
                    onSettingsChange(defaults)
                    onSettingsSettled(defaults)
                }
            ) { Text("重置") }
        }
        HorizontalDivider()

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text("动画", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            MotionSlider(
                label = "阻尼",
                valueLabel = String.format("%.2f", settings.dampingRatio),
                value = settings.dampingRatio,
                range = MotionSettings.MinDamping..MotionSettings.MaxDamping,
                onValueChange = { onSettingsChange(settings.copy(dampingRatio = it)) },
                onValueChangeFinished = { onSettingsSettled(settings) }
            )

            MotionSlider(
                label = "刚度",
                valueLabel = settings.stiffness.roundToInt().toString(),
                value = settings.stiffness,
                range = MotionSettings.MinStiffness..MotionSettings.MaxStiffness,
                onValueChange = { onSettingsChange(settings.copy(stiffness = it)) },
                onValueChangeFinished = { onSettingsSettled(settings) }
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .fillMaxWidth()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier
                            .offset(x = (previewProgress * 220).dp)
                            .size(width = 36.dp, height = 8.dp)
                    ) {}
                }
            }
        }
    }
}

@Composable
private fun MotionSlider(
    label: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(valueLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = range
        )
    }
}

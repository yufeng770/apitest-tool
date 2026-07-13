package com.example.yuapitest

import android.content.Context
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

data class MotionSettings(
    val dampingRatio: Float = DefaultDamping,
    val stiffness: Float = DefaultStiffness
) {
    fun normalized(): MotionSettings = copy(
        dampingRatio = dampingRatio.coerceIn(MinDamping, MaxDamping),
        stiffness = stiffness.coerceIn(MinStiffness, MaxStiffness)
    )

    companion object {
        const val MinDamping = 0.25f
        const val MaxDamping = 1.0f
        const val DefaultDamping = 0.82f
        const val MinStiffness = 100f
        const val MaxStiffness = 1200f
        const val DefaultStiffness = 620f
    }
}

fun <T> MotionSettings.springSpec(): SpringSpec<T> = spring(
    dampingRatio = dampingRatio,
    stiffness = stiffness
)

class MotionSettingsRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "motion_settings",
        Context.MODE_PRIVATE
    )

    fun load(): MotionSettings = MotionSettings(
        dampingRatio = preferences.getFloat("damping_ratio", MotionSettings.DefaultDamping),
        stiffness = preferences.getFloat("stiffness", MotionSettings.DefaultStiffness)
    ).normalized()

    fun save(settings: MotionSettings) {
        val normalized = settings.normalized()
        preferences.edit()
            .putFloat("damping_ratio", normalized.dampingRatio)
            .putFloat("stiffness", normalized.stiffness)
            .apply()
    }
}

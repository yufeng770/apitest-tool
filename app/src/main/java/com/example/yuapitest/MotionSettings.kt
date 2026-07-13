package com.example.yuapitest

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

private const val DefaultDamping = 0.82f
private const val DefaultStiffness = 620f

fun <T> defaultSpringSpec(): SpringSpec<T> = spring(
    dampingRatio = DefaultDamping,
    stiffness = DefaultStiffness
)

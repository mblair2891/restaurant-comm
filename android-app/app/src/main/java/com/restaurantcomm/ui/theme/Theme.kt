package com.restaurantcomm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryTeal,
    background = LightBackground
)

private val DarkColors = darkColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryTeal
)

@Composable
fun RestaurantCommTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content
    )
}

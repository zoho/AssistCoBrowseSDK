package com.zoho.assist.cobrowse.demo.common

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/**
 * The bank's Material 3 theme: colour, shape and system-bar appearance for every
 * screen (the Activity window uses the mirrored `Theme.Material3.DayNight` in
 * `styles.xml`).
 *
 * Colour is always the bundled [BankLightColors] / [BankDarkColors], a full M3
 * tonal scheme generated from the brand blue. Dynamic colour (Material You) is
 * deliberately not used: a bank keeps its brand colours on every device.
 */
@Composable
fun BankTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme: ColorScheme = if (darkTheme) BankDarkColors else BankLightColors

    // Edge-to-edge is enabled in MainActivity; the bar icons still have to be
    // flipped to match the scheme or they vanish against it.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = BankShapes,
        content = content,
    )
}

/** A rounder shape scale than the M3 baseline, shared by every surface in the app. */
private val BankShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

// Generated from the brand-blue seed. Every role is set: leaving one to default would mix in
// baseline-purple values.

private val BankLightColors = lightColorScheme(
    primary = Color(0xFF1F5BB7),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Color(0xFF001A42),
    inversePrimary = Color(0xFFB8C4FF),
    secondary = Color(0xFF5A5D71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDEE1F8),
    onSecondaryContainer = Color(0xFF171B2B),
    tertiary = Color(0xFF864C6B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8EB),
    onTertiaryContainer = Color(0xFF3A0426),
    error = Color(0xFFAA3529),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEDBD3),
    onErrorContainer = Color(0xFF3A0A00),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF1A1B21),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF1A1B21),
    surfaceVariant = Color(0xFFDFE1F5),
    onSurfaceVariant = Color(0xFF434655),
    surfaceTint = Color(0xFF1F5BB7),
    inverseSurface = Color(0xFF2F3036),
    inverseOnSurface = Color(0xFFEFF0F8),
    outline = Color(0xFF747687),
    outlineVariant = Color(0xFFC3C5D8),
    scrim = Color(0xFF000009),
    surfaceBright = Color(0xFFF8F9FF),
    surfaceDim = Color(0xFFD9D9E1),
    surfaceContainer = Color(0xFFECEDF5),
    surfaceContainerHigh = Color(0xFFE7E8EF),
    surfaceContainerHighest = Color(0xFFE1E2EA),
    surfaceContainerLow = Color(0xFFF2F3FB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    primaryFixed = Color(0xFFDDE1FF),
    primaryFixedDim = Color(0xFFB8C4FF),
    onPrimaryFixed = Color(0xFF001A42),
    onPrimaryFixedVariant = Color(0xFF004491),
    secondaryFixed = Color(0xFFDEE1F8),
    secondaryFixedDim = Color(0xFFC2C5DC),
    onSecondaryFixed = Color(0xFF171B2B),
    onSecondaryFixedVariant = Color(0xFF424658),
    tertiaryFixed = Color(0xFFFFD8EB),
    tertiaryFixedDim = Color(0xFFF6B3D5),
    onTertiaryFixed = Color(0xFF3A0426),
    onTertiaryFixedVariant = Color(0xFF6C3453),
)

private val BankDarkColors = darkColorScheme(
    primary = Color(0xFFB8C4FF),
    onPrimary = Color(0xFF002F68),
    primaryContainer = Color(0xFF004491),
    onPrimaryContainer = Color(0xFFDDE1FF),
    inversePrimary = Color(0xFF1F5BB7),
    secondary = Color(0xFFC2C5DC),
    onSecondary = Color(0xFF2C3041),
    secondaryContainer = Color(0xFF424658),
    onSecondaryContainer = Color(0xFFDEE1F8),
    tertiary = Color(0xFFF6B3D5),
    onTertiary = Color(0xFF531C3C),
    tertiaryContainer = Color(0xFF6C3453),
    onTertiaryContainer = Color(0xFFFFD8EB),
    error = Color(0xFFFFB4A5),
    onError = Color(0xFF6A0000),
    errorContainer = Color(0xFF8C1813),
    onErrorContainer = Color(0xFFFEDBD3),
    background = Color(0xFF121319),
    onBackground = Color(0xFFE1E2EA),
    surface = Color(0xFF121319),
    onSurface = Color(0xFFE1E2EA),
    surfaceVariant = Color(0xFF434655),
    onSurfaceVariant = Color(0xFFC3C5D8),
    surfaceTint = Color(0xFFB8C4FF),
    inverseSurface = Color(0xFFE1E2EA),
    inverseOnSurface = Color(0xFF2F3036),
    outline = Color(0xFF8D90A1),
    outlineVariant = Color(0xFF434655),
    scrim = Color(0xFF000009),
    surfaceBright = Color(0xFF38393F),
    surfaceDim = Color(0xFF121319),
    surfaceContainer = Color(0xFF1E1F25),
    surfaceContainerHigh = Color(0xFF292A2F),
    surfaceContainerHighest = Color(0xFF34343A),
    surfaceContainerLow = Color(0xFF1A1B21),
    surfaceContainerLowest = Color(0xFF0D0E15),
    primaryFixed = Color(0xFFDDE1FF),
    primaryFixedDim = Color(0xFFB8C4FF),
    onPrimaryFixed = Color(0xFF001A42),
    onPrimaryFixedVariant = Color(0xFF004491),
    secondaryFixed = Color(0xFFDEE1F8),
    secondaryFixedDim = Color(0xFFC2C5DC),
    onSecondaryFixed = Color(0xFF171B2B),
    onSecondaryFixedVariant = Color(0xFF424658),
    tertiaryFixed = Color(0xFFFFD8EB),
    tertiaryFixedDim = Color(0xFFF6B3D5),
    onTertiaryFixed = Color(0xFF3A0426),
    onTertiaryFixedVariant = Color(0xFF6C3453),
)

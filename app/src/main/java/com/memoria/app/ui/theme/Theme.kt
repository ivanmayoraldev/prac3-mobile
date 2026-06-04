package com.memoria.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp


object MemorIAColors {
    val IndigoDeep       = Color(0xFF1A1035)
    val IndigoDark       = Color(0xFF251847)
    val IndigoMid        = Color(0xFF3D2B7A)
    val IndigoLight      = Color(0xFF6B4FBB)
    val IndigoAccent     = Color(0xFF9B7DFF)

    val GoldBright       = Color(0xFFFFD166)
    val GoldMid          = Color(0xFFE8B84B)
    val GoldDark         = Color(0xFFA87D2A)

    val NeutralDark      = Color(0xFF0D0A1A)
    val NeutralMid       = Color(0xFF1C1830)
    val NeutralSurface   = Color(0xFF221E38)
    val NeutralCard      = Color(0xFF2A2544)
    val NeutralBorder    = Color(0xFF3A3558)
    val NeutralText      = Color(0xFFC8C0E8)
    val NeutralHint      = Color(0xFF7A72A0)

    val Success          = Color(0xFF4ADE80)
    val Warning          = Color(0xFFFBBF24)
    val Error            = Color(0xFFF87171)
    val Info             = Color(0xFF60A5FA)

    val LightBackground  = Color(0xFFF5F3FF)
    val LightSurface     = Color(0xFFFFFFFF)
    val LightCard        = Color(0xFFF0EDFF)
    val LightBorder      = Color(0xFFD4CEFF)
    val LightText        = Color(0xFF1A1035)
}

private val DarkColorScheme = darkColorScheme(
    primary          = MemorIAColors.IndigoAccent,
    onPrimary        = MemorIAColors.IndigoDeep,
    primaryContainer = MemorIAColors.IndigoMid,
    onPrimaryContainer = MemorIAColors.NeutralText,
    secondary        = MemorIAColors.GoldBright,
    onSecondary      = MemorIAColors.IndigoDeep,
    secondaryContainer = MemorIAColors.GoldDark,
    onSecondaryContainer = MemorIAColors.GoldBright,
    tertiary         = MemorIAColors.Info,
    background       = MemorIAColors.NeutralDark,
    onBackground     = MemorIAColors.NeutralText,
    surface          = MemorIAColors.NeutralMid,
    onSurface        = MemorIAColors.NeutralText,
    surfaceVariant   = MemorIAColors.NeutralCard,
    onSurfaceVariant = MemorIAColors.NeutralHint,
    outline          = MemorIAColors.NeutralBorder,
    error            = MemorIAColors.Error,
    onError          = MemorIAColors.IndigoDeep,
)

private val LightColorScheme = lightColorScheme(
    primary          = MemorIAColors.IndigoMid,
    onPrimary        = Color.White,
    primaryContainer = MemorIAColors.LightCard,
    onPrimaryContainer = MemorIAColors.IndigoDeep,
    secondary        = MemorIAColors.GoldDark,
    onSecondary      = Color.White,
    secondaryContainer = Color(0xFFFFF4D6),
    onSecondaryContainer = MemorIAColors.GoldDark,
    background       = MemorIAColors.LightBackground,
    onBackground     = MemorIAColors.LightText,
    surface          = MemorIAColors.LightSurface,
    onSurface        = MemorIAColors.LightText,
    surfaceVariant   = MemorIAColors.LightCard,
    onSurfaceVariant = MemorIAColors.IndigoLight,
    outline          = MemorIAColors.LightBorder,
    error            = MemorIAColors.Error,
    onError          = Color.White,
)

val MemorIATypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize   = 57.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 45.sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 32.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 28.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 24.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 22.sp,
        letterSpacing = (-0.25).sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 16.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 14.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 16.sp,
        lineHeight  = 24.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
        lineHeight  = 20.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 12.sp,
        lineHeight  = 16.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 14.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 12.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 11.sp,
        letterSpacing = 0.5.sp
    ),
)

@Composable
fun MemorIATheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = MemorIATypography,
        content     = content
    )
}

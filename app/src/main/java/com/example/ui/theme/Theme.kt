package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.data.model.AccentTheme
import com.example.data.model.AppThemeConfig
import com.example.data.model.ThemeMode

data class ExtendedColors(
    val cardBackground: Color,
    val cardBorder: Color,
    val subCardBackground: Color,
    val heroBackground: Color,
    val heroText: Color,
    val heroSubCardBg: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val dcColor: Color,
    val acColor: Color,
    val costOrange: Color,
    val rangeCyan: Color
)

val LocalExtendedColors = compositionLocalOf {
    ExtendedColors(
        cardBackground = GeometricSurfaceLight,
        cardBorder = GeometricCardBorderLight,
        subCardBackground = GeometricSubCardLight,
        heroBackground = GeometricHeroBgLight,
        heroText = GeometricHeroTextLight,
        heroSubCardBg = Color.White.copy(alpha = 0.5f),
        accentPrimary = GeometricTeal,
        accentSecondary = GeometricTealBright,
        textPrimary = TextPrimaryLight,
        textSecondary = TextSecondaryLight,
        textMuted = TextMutedLight,
        dcColor = ChargeDCColor,
        acColor = ChargeACColor,
        costOrange = CostOrange,
        rangeCyan = RangeCyan
    )
}

@Composable
fun EVAppTheme(
    themeConfig: AppThemeConfig = AppThemeConfig(),
    content: @Composable () -> Unit
) {
    val isDark = when (themeConfig.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val (primaryColor, secondaryColor) = when (themeConfig.accentTheme) {
        AccentTheme.GEOMETRIC_TEAL -> Pair(GeometricTeal, GeometricTealBright)
        AccentTheme.TECH_BLUE -> Pair(ElectricBlue, ElectricBlueBright)
        AccentTheme.ECO_MINT -> Pair(EcoMint, EcoMintBright)
        AccentTheme.AMBER_SUNSET -> Pair(AmberSunset, AmberSunsetBright)
        AccentTheme.NEON_VIOLET -> Pair(NeonViolet, NeonVioletBright)
        AccentTheme.GRAPHITE_DARK -> Pair(GraphiteDark, GraphiteBright)
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            secondary = secondaryColor,
            onSecondary = Color.Black,
            tertiary = secondaryColor,
            background = GeometricBgDark,
            onBackground = TextPrimaryDark,
            surface = GeometricSurfaceDark,
            onSurface = TextPrimaryDark,
            surfaceVariant = GeometricCardDark,
            onSurfaceVariant = TextSecondaryDark
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            secondary = secondaryColor,
            onSecondary = Color.White,
            tertiary = primaryColor,
            background = GeometricBgLight,
            onBackground = TextPrimaryLight,
            surface = GeometricSurfaceLight,
            onSurface = TextPrimaryLight,
            surfaceVariant = GeometricSubCardLight,
            onSurfaceVariant = TextSecondaryLight
        )
    }

    val extendedColors = if (isDark) {
        ExtendedColors(
            cardBackground = GeometricCardDark,
            cardBorder = GeometricCardBorderDark,
            subCardBackground = GeometricSubCardDark,
            heroBackground = GeometricHeroBgDark,
            heroText = GeometricHeroTextDark,
            heroSubCardBg = Color.White.copy(alpha = 0.1f),
            accentPrimary = primaryColor,
            accentSecondary = secondaryColor,
            textPrimary = TextPrimaryDark,
            textSecondary = TextSecondaryDark,
            textMuted = TextMutedDark,
            dcColor = ChargeDCColor,
            acColor = ChargeACColor,
            costOrange = CostOrange,
            rangeCyan = secondaryColor
        )
    } else {
        ExtendedColors(
            cardBackground = GeometricSurfaceLight,
            cardBorder = GeometricCardBorderLight,
            subCardBackground = GeometricSubCardLight,
            heroBackground = GeometricHeroBgLight,
            heroText = GeometricHeroTextLight,
            heroSubCardBg = Color.White.copy(alpha = 0.5f),
            accentPrimary = primaryColor,
            accentSecondary = secondaryColor,
            textPrimary = TextPrimaryLight,
            textSecondary = TextSecondaryLight,
            textMuted = TextMutedLight,
            dcColor = ChargeDCColor,
            acColor = ChargeACColor,
            costOrange = CostOrange,
            rangeCyan = RangeCyan
        )
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

package com.example.gymlocker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.gymlocker.data.entity.AppTheme

// ============================================================
// "System matches Default" theme rules
// - background / surface / surfaceVariant are the reliable baseline layers
// - tertiary is an ACCENT only (never assumed as main card surface)
// - DEFAULT keeps dynamic colors (Material You) when available
// ============================================================

private fun lightCompactScheme(
    primary: Color,
    onPrimary: Color,
    secondary: Color,
    onSecondary: Color,
    tertiary: Color,
    onTertiary: Color,
    background: Color,
    onBackground: Color,
    surface: Color,
    onSurface: Color,
    surfaceVariant: Color = surface,
    onSurfaceVariant: Color = onSurface,
    outline: Color? = null
) = lightColorScheme(
    primary = primary,
    onPrimary = onPrimary,

    secondary = secondary,
    onSecondary = onSecondary,

    tertiary = tertiary,
    onTertiary = onTertiary,

    background = background,
    onBackground = onBackground,

    surface = surface,
    onSurface = onSurface,

    surfaceVariant = surfaceVariant,
    onSurfaceVariant = onSurfaceVariant,

    outline = outline ?: onSurface.copy(alpha = 0.35f)
)

private fun darkCompactScheme(
    primary: Color,
    onPrimary: Color,
    secondary: Color,
    onSecondary: Color,
    tertiary: Color,
    onTertiary: Color,
    background: Color,
    onBackground: Color,
    surface: Color,
    onSurface: Color,
    surfaceVariant: Color = surface,
    onSurfaceVariant: Color = onSurface,
    outline: Color? = null
) = darkColorScheme(
    primary = primary,
    onPrimary = onPrimary,

    secondary = secondary,
    onSecondary = onSecondary,

    tertiary = tertiary,
    onTertiary = onTertiary,

    background = background,
    onBackground = onBackground,

    surface = surface,
    onSurface = onSurface,

    surfaceVariant = surfaceVariant,
    onSurfaceVariant = onSurfaceVariant,

    outline = outline ?: onSurface.copy(alpha = 0.35f)
)

// ---------- DEFAULT (your old fallback schemes) ----------
// These are only used when AppTheme.DEFAULT AND dynamicColor is not used/available.
private val DefaultFallbackDark = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val DefaultFallbackLight = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

// ---------- RED ----------
private val RedLight = lightCompactScheme(
    // Muted locker red (accents / buttons)
    primary = Color(0xFF8B1E1A),
    onPrimary = Color(0xFFFFFFFF),

    // Steel / hardware grey
    secondary = Color(0xFF4A4F55),
    onSecondary = Color(0xFFFFFFFF),

    // Bench wood (warm accent)
    tertiary = Color(0xFFCEB79F),
    onTertiary = Color(0xFF1F1510),

    // Cool, clean locker-room light
    background = Color(0xFFF4F5F7),
    onBackground = Color(0xFF141517),

    // Locker metal panels
    surface = Color(0xFFE6E7EA),
    onSurface = Color(0xFF141517),

    // Slightly deeper metal for inputs/cards separation
    surfaceVariant = Color(0xFFD6D8DD),
    onSurfaceVariant = Color(0xFF2B2D30),

    outline = Color(0xFF9AA0A6)
)
private val RedDark = darkCompactScheme(
    // Muted red highlight (still “red theme” but not neon)
    primary = Color(0xFFB83A35),
    onPrimary = Color(0xFF140707),

    // Cool grey highlight
    secondary = Color(0xFF9AA0A6),
    onSecondary = Color(0xFF101214),

    // Bench wood accent (warm pop in dark UI)
    tertiary = Color(0xFFD8C1A6),
    onTertiary = Color(0xFF1B140E),

    // Charcoal floor / deep room darkness
    background = Color(0xFF0B0C0D),
    onBackground = Color(0xFFECEDEF),

    // Dark steel surfaces
    surface = Color(0xFF141619),
    onSurface = Color(0xFFECEDEF),

    // Slightly lighter steel for variants/inputs
    surfaceVariant = Color(0xFF1E2125),
    onSurfaceVariant = Color(0xFFC9CDD3),

    outline = Color(0xFF5E646B)
)


// ---------- BLUE ----------
private val BlueLight = lightCompactScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color(0xFFFFFFFF),

    secondary = Color(0xFF4E5BA6),
    onSecondary = Color(0xFFFFFFFF),

    tertiary = Color(0xFFE7F0F6),          // accent container
    onTertiary = Color(0xFF0E1B24),

    background = Color(0xFFF7FBFE),
    onBackground = Color(0xFF0E1B24),

    surface = Color(0xFFE7F0F6),
    onSurface = Color(0xFF0E1B24),

    surfaceVariant = Color(0xFFDCE7EE),
    onSurfaceVariant = Color(0xFF0E1B24),

    outline = Color(0xFF9FB2BE)
)

private val BlueDark = darkCompactScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF061622),

    secondary = Color(0xFFC2C5FF),
    onSecondary = Color(0xFF0E1024),

    tertiary = Color(0xFF0E1A21),          // accent container
    onTertiary = Color(0xFFE7F2F5),

    background = Color(0xFF060C10),
    onBackground = Color(0xFFE7F2F5),

    surface = Color(0xFF0E1A21),
    onSurface = Color(0xFFE7F2F5),

    surfaceVariant = Color(0xFF122634),
    onSurfaceVariant = Color(0xFFE7F2F5),

    outline = Color(0xFF4B6776)
)

// ---------- GREEN ----------
private val GreenLight = lightCompactScheme(
    primary = Color(0xFF1B7A4B),          // bog emerald
    onPrimary = Color(0xFFFFFFFF),

    secondary = Color(0xFF3F6D52),        // moss green
    onSecondary = Color(0xFFFFFFFF),

    tertiary = Color(0xFF9A7B2F),         // ancient amber/gold accent
    onTertiary = Color(0xFF1A1206),

    background = Color(0xFFF3F7F3),       // swamp mist
    onBackground = Color(0xFF0E1A14),

    surface = Color(0xFFE6EEE7),          // wet stone
    onSurface = Color(0xFF0E1A14),

    surfaceVariant = Color(0xFFD3DED4),   // mossy slab / input fill
    onSurfaceVariant = Color(0xFF2B3B32),

    outline = Color(0xFF6F8277)
)

private val GreenDark = darkCompactScheme(
    primary = Color(0xFFA5D6A7),
    onPrimary = Color(0xFF061622),

    secondary = Color(0xFFB7CCBC),
    onSecondary = Color(0xFF05201F),

    tertiary = Color(0xFF0E1A21),
    onTertiary = Color(0xFFE7F2F5),

    background = Color(0xFF060C10),
    onBackground = Color(0xFFE7F2F5),

    surface = Color(0xFF0E1A21),
    onSurface = Color(0xFFE7F2F5),

    surfaceVariant = Color(0xFF122634),
    onSurfaceVariant = Color(0xFFE7F2F5),

    outline = Color(0xFF4B6776)
)

// ---------- RETRO ----------
private val ArcadeLight = lightCompactScheme(
    primary = Color(0xFFFF00E5),
    onPrimary = Color(0xFF000000),

    secondary = Color(0xFF4DFAFF),
    onSecondary = Color(0xFFFFFFFF),

    tertiary = Color(0xFFF7F2FF),          // accent container
    onTertiary = Color(0xFF1B1024),

    background = Color(0xFFFCF7FF),
    onBackground = Color(0xFF1B1024),

    surface = Color(0xFFF7F2FF),
    onSurface = Color(0xFF1B1024),

    surfaceVariant = Color(0xFFF0E8FF),
    onSurfaceVariant = Color(0xFF1B1024),

    outline = Color(0xFFB9A6D6)
)

private val ArcadeDark = darkCompactScheme(
    primary = Color(0xFFFF4DEB),
    onPrimary = Color(0xFF1A0017),

    secondary = Color(0xFF4DFAFF),
    onSecondary = Color(0xFF002022),

    tertiary = Color(0xFF1A1024),          // accent container
    onTertiary = Color(0xFFF4EFFF),

    background = Color(0xFF08040B),
    onBackground = Color(0xFFF4EFFF),

    surface = Color(0xFF0D0712),
    onSurface = Color(0xFFF4EFFF),

    surfaceVariant = Color(0xFF151020),
    onSurfaceVariant = Color(0xFFF4EFFF),

    outline = Color(0xFF6C5A83)
)

// ---------- SPONGEBOB (explicit) ----------
private val SpongeBobLight = lightColorScheme(
    primary = Color(0xFFFFD400),
    onPrimary = Color(0xFF2A1F00),

    secondary = Color(0xFF0077C8),
    onSecondary = Color(0xFFFFFFFF),

    tertiary = Color(0xFFFFF1B8),       // accent container
    onTertiary = Color(0xFF2A1F00),

    background = Color(0xFFE6FAFF),
    onBackground = Color(0xFF1F1B16),

    surface = Color(0xFFFFF6D9),
    onSurface = Color(0xFF1F1B16),

    surfaceVariant = Color(0xFFFFEDBF),
    onSurfaceVariant = Color(0xFF1F1B16),

    outline = Color(0xFF2CB67D)
)

private val SpongeBobDark = darkColorScheme(
    primary = Color(0xFFFFE066),
    onPrimary = Color(0xFF2A1F00),

    secondary = Color(0xFF6EC6FF),
    onSecondary = Color(0xFF001F2A),

    tertiary = Color(0xFFB7F3FF),       // accent container
    onTertiary = Color(0xFF001F2A),

    background = Color(0xFF041015),
    onBackground = Color(0xFFFFF4D9),

    surface = Color(0xFF0B1E26),
    onSurface = Color(0xFFFFF4D9),

    surfaceVariant = Color(0xFF12313D),
    onSurfaceVariant = Color(0xFFFFF4D9),

    outline = Color(0xFFFFD400)
)

// ---------- SPIDERMAN (explicit) ----------
private val SpiderManLight = lightColorScheme(
    primary = Color(0xFFE53935),
    onPrimary = Color(0xFFFFFFFF),

    secondary = Color(0xFF1E5AA8),
    onSecondary = Color(0xFFFFFFFF),

    tertiary = Color(0xFFFFD6D6),       // accent container (soft comic tint)
    onTertiary = Color(0xFF0B1220),

    background = Color(0xFFBFE7FF),
    onBackground = Color(0xFF0B1220),

    surface = Color(0xFFF8FBFF),
    onSurface = Color(0xFF0B1220),

    surfaceVariant = Color(0xFFE7F2FF),
    onSurfaceVariant = Color(0xFF0B1220),

    outline = Color(0xFF0B1220)
)

private val SpiderManDark = darkColorScheme(
    primary = Color(0xFFFF4D4D),
    onPrimary = Color(0xFF200002),

    secondary = Color(0xFF4DA3FF),
    onSecondary = Color(0xFF00121F),

    tertiary = Color(0xFF1B0F14),       // accent container (deep red-black)
    onTertiary = Color(0xFFECEFF1),

    background = Color(0xFF060A12),
    onBackground = Color(0xFFECEFF1),

    surface = Color(0xFF0E1A21),
    onSurface = Color(0xFFECEFF1),

    surfaceVariant = Color(0xFF122634),
    onSurfaceVariant = Color(0xFFECEFF1),

    outline = Color(0xFFECEFF1)
)

// ---------- MATRIX (explicit special/semantic) ----------
private val MatrixDark = darkColorScheme(
    primary = Color(0xFF00FF41),
    secondary = Color(0xFF00C853),
    tertiary = Color(0xFF000000),       // accent exists but baseline is background/surface

    background = Color(0xFF000000),
    surface = Color(0xFF050505),
    surfaceVariant = Color(0xFF0B0B0B),

    onPrimary = Color(0xFF000000),
    onSecondary = Color(0xFF000000),
    onTertiary = Color(0xFF00FF41),

    onBackground = Color(0xFF00FF41),
    onSurface = Color(0xFF00FF41),
    onSurfaceVariant = Color(0xFF00E676),

    outline = Color(0xFF00FF41)
)

// ============================================================
// SWAMP THEME — LIGHT
// Deep Ancient Swamp (misty daylight)
// ============================================================

val SwampLightColors = lightColorScheme(
    primary = Color(0xFF1B7A4B),          // bog emerald
    onPrimary = Color(0xFFFFFFFF),

    secondary = Color(0xFF3F6D52),        // moss green
    onSecondary = Color(0xFFFFFFFF),

    tertiary = Color(0xFF9A7B2F),         // ancient amber/gold accent
    onTertiary = Color(0xFF1A1206),

    background = Color(0xFFF3F7F3),       // swamp mist
    onBackground = Color(0xFF0E1A14),

    surface = Color(0xFFE6EEE7),          // wet stone
    onSurface = Color(0xFF0E1A14),

    surfaceVariant = Color(0xFFD3DED4),   // mossy slab / input fill
    onSurfaceVariant = Color(0xFF2B3B32),

    outline = Color(0xFF6F8277)
)

// ============================================================
// SWAMP THEME — DARK
// Deep Ancient Swamp (night / peat + bioluminescence)
// ============================================================

val SwampDarkColors = darkColorScheme(
    primary = Color(0xFF38D987),          // bioluminescent swamp green
    onPrimary = Color(0xFF062014),

    secondary = Color(0xFF6FB58A),        // moss highlight
    onSecondary = Color(0xFF071A10),

    tertiary = Color(0xFFD0A84A),         // rune amber accent
    onTertiary = Color(0xFF1C1204),

    background = Color(0xFF07130E),       // deep peat water
    onBackground = Color(0xFFE7F2EA),

    surface = Color(0xFF0C1D16),          // moss rock (slightly lighter than bg)
    onSurface = Color(0xFFE7F2EA),

    surfaceVariant = Color(0xFF143125),   // input / chips base
    onSurfaceVariant = Color(0xFFBFD3C6),

    outline = Color(0xFF496357)
)

// ---------- Shapes ----------
private val DefaultShapes = Shapes()

private val SpongeBobShapes = Shapes(
    extraSmall = RoundedCornerShape(24.dp),
    small = RoundedCornerShape(28.dp),
    medium = RoundedCornerShape(32.dp),
    large = RoundedCornerShape(36.dp),
    extraLarge = RoundedCornerShape(40.dp),
)

private val SpiderManShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(22.dp),
)

@Composable
fun GymLockerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val settings = com.example.gymlocker.ui.settings.LocalUserSettings.current
    val effectiveDarkTheme = settings.forceDarkMode || darkTheme

    val shapes = when (settings.appTheme) {
        AppTheme.SpongeBob -> SpongeBobShapes
        AppTheme.SpiderMan -> SpiderManShapes
        else -> DefaultShapes
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !effectiveDarkTheme
            controller.isAppearanceLightNavigationBars = !effectiveDarkTheme
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
        }
    }

    // 1) Pick the base scheme (as you already did)
    val baseScheme = when (settings.appTheme) {
        AppTheme.DEFAULT -> when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (effectiveDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            else -> if (effectiveDarkTheme) DefaultFallbackDark else DefaultFallbackLight
        }

        AppTheme.RED -> if (effectiveDarkTheme) RedDark else RedLight
        AppTheme.BLUE -> if (effectiveDarkTheme) BlueDark else BlueLight
        AppTheme.GREEN -> if (effectiveDarkTheme) GreenDark else GreenLight
        AppTheme.ARCADE -> if (effectiveDarkTheme) ArcadeDark else ArcadeLight
        AppTheme.SpongeBob -> if (effectiveDarkTheme) SpongeBobDark else SpongeBobLight
        AppTheme.SpiderMan -> if (effectiveDarkTheme) SpiderManDark else SpiderManLight
        AppTheme.Swamp -> if (effectiveDarkTheme) SwampDarkColors else SwampLightColors
        AppTheme.MATRIX -> MatrixDark
    }

    // 2) Global safety net: if surface == background, promote surfaceVariant to surface
    val scheme = baseScheme.copy(
        background = baseScheme.background,
        surface = if (baseScheme.surface == baseScheme.background)
            baseScheme.surfaceVariant
        else baseScheme.surface
    )

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        shapes = shapes,
        content = content
    )
}

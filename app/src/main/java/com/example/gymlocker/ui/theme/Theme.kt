package com.example.gymlocker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
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
import androidx.core.view.WindowCompat
import com.example.gymlocker.data.entity.AppTheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// ---------- Compact helpers (SeaBreeze system) ----------

private fun lightCompactScheme(
    primary: Color,
    onPrimary: Color,
    secondary: Color,
    onSecondary: Color,
    tertiary: Color,
    onTertiary: Color,
    background: Color = tertiary,
    onBackground: Color = onTertiary,
    surface: Color = tertiary,
    onSurface: Color = onTertiary,
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
    surfaceVariant = tertiary,
    onSurfaceVariant = onTertiary,
    outline = outline ?: onTertiary.copy(alpha = 0.35f)
)

private fun darkCompactScheme(
    primary: Color,
    onPrimary: Color,
    secondary: Color,
    onSecondary: Color,
    tertiary: Color,
    onTertiary: Color,
    background: Color = tertiary,
    onBackground: Color = onTertiary,
    surface: Color = tertiary,
    onSurface: Color = onTertiary,
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
    surfaceVariant = tertiary,
    onSurfaceVariant = onTertiary,
    outline = outline ?: onTertiary.copy(alpha = 0.35f)
)

// ---------- DEFAULT (Material defaults you already had) ----------
// Keep these as-is (or convert later if you want). They’re only used when AppTheme.DEFAULT + not dynamic.
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

// ---------- RED (now matches SeaBreeze system: on*, background/surface, surfaceVariant, outline) ----------
private val RedLight = lightCompactScheme(
    primary = Color(0xFFB3261E),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF7D5260),
    onSecondary = Color(0xFFFFFFFF),

    // Keep red theme as-is (already readable)
    tertiary = Color(0xFFFFFBFE),
    onTertiary = Color(0xFF1C1B1F),
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    outline = Color(0xFF8C8C8C)
)

private val RedDark = darkCompactScheme(
    primary = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF410002),
    secondary = Color(0xFFEFB8C8),
    onSecondary = Color(0xFF2D151C),
    tertiary = Color(0xFF121212),
    onTertiary = Color(0xFFE6E1E5),
    background = Color(0xFF0F0F0F),
    surface = Color(0xFF121212),
    outline = Color(0xFF6A6A6A)
)

// ---------- BLUE ----------
private val BlueLight = lightCompactScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color(0xFFFFFFFF),

    secondary = Color(0xFF4E5BA6),
    onSecondary = Color(0xFFFFFFFF),

    // ✅ More contrast: cards/surfaces darker than background
    tertiary = Color(0xFFE7F0F6),     // card surface / containers (darker)
    onTertiary = Color(0xFF0E1B24),

    background = Color(0xFFF7FBFE),   // screen background (lighter)
    surface = Color(0xFFE7F0F6),      // cards/sheets/inputs (darker)
    outline = Color(0xFF9FB2BE)
)

private val BlueDark = darkCompactScheme(
    primary = Color(0xFF90CAF9),      // light blue
    onPrimary = Color(0xFF061622),

    secondary = Color(0xFFC2C5FF),    // soft indigo
    onSecondary = Color(0xFF0E1024),

    // ✅ Cards/surfaces should be lighter than background
    tertiary = Color(0xFF0E1A21),     // card surface / containers
    onTertiary = Color(0xFFE7F2F5),   // foam text

    background = Color(0xFF060C10),   // deeper screen background
    surface = Color(0xFF0E1A21),      // cards/sheets/inputs
    outline = Color(0xFF4B6776)       // subtle outline
)


// ---------- GREEN (adjusted to match Blue separation system) ----------
private val GreenLight = lightCompactScheme(
    primary = Color(0xFF2E7D32),      // strong green
    onPrimary = Color(0xFFFFFFFF),

    secondary = Color(0xFF4E6356),    // muted forest/sage
    onSecondary = Color(0xFFFFFFFF),

    // ✅ Match BlueLight separation
    tertiary = Color(0xFFE7F0F6),
    onTertiary = Color(0xFF0E1B24),

    background = Color(0xFFF7FBFE),
    surface = Color(0xFFE7F0F6),
    outline = Color(0xFF9FB2BE)
)

private val GreenDark = darkCompactScheme(
    primary = Color(0xFFA5D6A7),      // light green
    onPrimary = Color(0xFF061622),

    secondary = Color(0xFFB7CCBC),    // soft sage
    onSecondary = Color(0xFF05201F),

    // ✅ Match BlueDark separation
    tertiary = Color(0xFF0E1A21),
    onTertiary = Color(0xFFE7F2F5),

    background = Color(0xFF060C10),
    surface = Color(0xFF0E1A21),
    outline = Color(0xFF4B6776)
)

// ---------- RETRO ----------
private val RetroArcadeLight = lightCompactScheme(
    primary = Color(0xFFFF00E5),      // neon magenta
    onPrimary = Color(0xFF000000),
    secondary = Color(0xFF002AFF),    // neon blue
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFFF7F2FF),     // soft lilac-ish surface
    onTertiary = Color(0xFF1B1024),
    background = Color(0xFFFCF7FF),
    surface = Color(0xFFF7F2FF),
    outline = Color(0xFFB9A6D6)
)

private val RetroArcadeDark = darkCompactScheme(
    primary = Color(0xFFFF4DEB),
    onPrimary = Color(0xFF1A0017),
    secondary = Color(0xFF4DFAFF),
    onSecondary = Color(0xFF002022),
    tertiary = Color(0xFF0D0712),
    onTertiary = Color(0xFFF4EFFF),
    background = Color(0xFF08040B),
    surface = Color(0xFF0D0712),
    outline = Color(0xFF6C5A83)
)

// ---------- SPONGEBOB ----------
// ---------- SPONGEBOB (Matrix-style explicit surfaces) ----------
private val SpongeBobLight = lightColorScheme(
    primary = Color(0xFFFFD400),        // sponge yellow
    onPrimary = Color(0xFF2A1F00),      // brown ink (cartoony)

    secondary = Color(0xFF0077C8),      // ocean blue
    onSecondary = Color(0xFFFFFFFF),

    tertiary = Color(0xFFFFF1B8),       // soft warm highlight
    onTertiary = Color(0xFF2A1F00),

    background = Color(0xFFE6FAFF),     // sky/aqua background
    onBackground = Color(0xFF1F1B16),

    surface = Color(0xFFFFF6D9),        // sand/cream cards
    onSurface = Color(0xFF1F1B16),

    surfaceVariant = Color(0xFFFFEDBF), // slightly deeper sand
    onSurfaceVariant = Color(0xFF1F1B16),

    outline = Color(0xFF2CB67D)         // seaweed green outline (gimmick)
)

private val SpongeBobDark = darkColorScheme(
    primary = Color(0xFFFFE066),        // readable yellow
    onPrimary = Color(0xFF2A1F00),

    secondary = Color(0xFF6EC6FF),      // light ocean
    onSecondary = Color(0xFF001F2A),

    tertiary = Color(0xFFB7F3FF),       // bubble highlight
    onTertiary = Color(0xFF001F2A),

    background = Color(0xFF041015),     // deep ocean
    onBackground = Color(0xFFFFF4D9),

    surface = Color(0xFF0B1E26),        // ocean surface (cards)
    onSurface = Color(0xFFFFF4D9),

    surfaceVariant = Color(0xFF12313D), // slightly lighter for controls
    onSurfaceVariant = Color(0xFFFFF4D9),

    outline = Color(0xFFFFD400)         // yellow outline pop (gimmick)
)

// ---------- SPIDERMAN (Matrix-style explicit surfaces) ----------
// ---------- SPIDERMAN (Light: classic cartoon daytime) ----------
private val SpiderManLight = lightColorScheme(
    primary = Color(0xFFE53935),        // Spidey red
    onPrimary = Color(0xFFFFFFFF),

    secondary = Color(0xFF1E5AA8),      // Spidey suit blue (slightly brighter than navy)
    onSecondary = Color(0xFFFFFFFF),

    tertiary = Color(0xFFFFFFFF),       // panel white (cards can use surface instead)
    onTertiary = Color(0xFF0B1220),     // deep ink

    background = Color(0xFFBFE7FF),     // sky blue (the vibe)
    onBackground = Color(0xFF0B1220),   // comic ink

    surface = Color(0xFFF8FBFF),        // bright “panel” surface
    onSurface = Color(0xFF0B1220),

    surfaceVariant = Color(0xFFE7F2FF), // soft panel tint for inputs/toggles
    onSurfaceVariant = Color(0xFF0B1220),

    outline = Color(0xFF0B1220)         // web-line ink (strong gimmick)
)


private val SpiderManDark = darkColorScheme(
    primary = Color(0xFFFF4D4D),        // punchy comic red
    onPrimary = Color(0xFF200002),

    secondary = Color(0xFF4DA3FF),      // electric spider blue
    onSecondary = Color(0xFF00121F),

    tertiary = Color(0xFFECEFF1),       // web white
    onTertiary = Color(0xFF081018),

    background = Color(0xFF060A12),     // deep night navy
    onBackground = Color(0xFFECEFF1),

    surface = Color(0xFF0E1A21),        // card surface (your dark separation pattern)
    onSurface = Color(0xFFECEFF1),

    surfaceVariant = Color(0xFF122634), // slightly lighter for controls
    onSurfaceVariant = Color(0xFFECEFF1),

    outline = Color(0xFFECEFF1)         // “web” outline pop (gimmick)
)


// ---------- MATRIX (keep explicit because it’s intentionally special/semantic) ----------
private val MatrixDark = darkColorScheme(
    primary = Color(0xFF00FF41),
    secondary = Color(0xFF00C853),
    tertiary = Color(0xFF000000),

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

    val colorScheme = when (settings.appTheme) {
        AppTheme.DEFAULT -> when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (effectiveDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            else -> if (effectiveDarkTheme) DarkColorScheme else LightColorScheme
        }

        AppTheme.RED -> if (effectiveDarkTheme) RedDark else RedLight
        AppTheme.BLUE -> if (effectiveDarkTheme) BlueDark else BlueLight
        AppTheme.GREEN -> if (effectiveDarkTheme) GreenDark else GreenLight
        AppTheme.RETRO -> if (effectiveDarkTheme) RetroArcadeDark else RetroArcadeLight
        AppTheme.SpongeBob -> if (effectiveDarkTheme) SpongeBobDark else SpongeBobLight
        AppTheme.SpiderMan -> if (effectiveDarkTheme) SpiderManDark else SpiderManLight
        AppTheme.MATRIX -> MatrixDark
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = shapes,
        content = content
    )
}

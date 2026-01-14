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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.gymlocker.data.entity.AppTheme

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)
private val RedLight = lightColorScheme(
    primary = Color(0xFFB3261E),
    secondary = Color(0xFF7D5260),
    tertiary = Color(0xFFB5832A)
)

private val RedDark = darkColorScheme(
    primary = Color(0xFFFFB4AB),
    secondary = Color(0xFFEFB8C8),
    tertiary = Color(0xFFFFDDB3)
)

// Same idea for Blue and Green
private val BlueLight = lightColorScheme(
    primary = Color(0xFF1565C0),    // strong blue
    secondary = Color(0xFF4E5BA6),  // muted indigo
    tertiary = Color(0xFF00838F)    // teal accent
)

private val BlueDark = darkColorScheme(
    primary = Color(0xFF90CAF9),    // light blue
    secondary = Color(0xFFC2C5FF),  // soft indigo
    tertiary = Color(0xFF73D6E3)    // bright teal accent
)

private val GreenLight = lightColorScheme(
    primary = Color(0xFF2E7D32),    // strong green
    secondary = Color(0xFF4E6356),  // muted “forest / sage” neutral
    tertiary = Color(0xFF006A6A)    // deep teal-green accent
)

private val GreenDark = darkColorScheme(
    primary = Color(0xFFA5D6A7),    // light green
    secondary = Color(0xFFB7CCBC),  // soft sage
    tertiary = Color(0xFF5BD7D7)    // cyan/teal pop
)

private val MatrixDark = darkColorScheme(
    primary = Color(0xFF00FF41),     // Neon green
    secondary = Color(0xFF00C853),   // Slightly darker neon green
    tertiary = Color(0xFF76FF03),    // Lime neon

    background = Color(0xFF000000),  // True black
    surface = Color(0xFF050505),     // Almost black (cards)
    surfaceVariant = Color(0xFF0B0B0B),

    onPrimary = Color(0xFF000000),
    onSecondary = Color(0xFF000000),
    onTertiary = Color(0xFF000000),

    onBackground = Color(0xFF00FF41), // Neon text
    onSurface = Color(0xFF00FF41),
    onSurfaceVariant = Color(0xFF00E676),

    outline = Color(0xFF00FF41)
)
private val RetroArcadeLight = lightColorScheme(
    primary = Color(0xFFFF00E5),   // neon magenta
    secondary = Color(0xFF00E5FF), // neon cyan
    tertiary = Color(0xFF7C4DFF)   // arcade purple
)

private val RetroArcadeDark = darkColorScheme(
    primary = Color(0xFFFF4DEB),   // softened neon magenta
    secondary = Color(0xFF4DFAFF), // softened neon cyan
    tertiary = Color(0xFFB69CFF)   // light neon purple
)
private val SpongeBobLight = lightColorScheme(
    primary = Color(0xFFFFD400),   // sponge yellow
    secondary = Color(0xFF0077C8), // ocean/sky blue
    tertiary = Color(0xFF8D5A2B)   // brown accent (pants/patty vibe)
)

private val SpongeBobDark = darkColorScheme(
    primary = Color(0xFFFFE066),   // light yellow (readable on dark)
    secondary = Color(0xFF6EC6FF), // light ocean blue
    tertiary = Color(0xFFD7A36B)   // warm tan accent
)
private val SpiderManLight = lightColorScheme(
    primary = Color(0xFFE53935),   // spider red
    secondary = Color(0xFF1E3A8A), // deep spider blue
    tertiary = Color(0xFFB0BEC5)   // web silver/grey
)

private val SpiderManDark = darkColorScheme(
    primary = Color(0xFFFF6B6B),   // brighter red for dark theme
    secondary = Color(0xFF90CAF9), // light blue for contrast
    tertiary = Color(0xFFECEFF1)   // web white
)


@Composable
fun GymLockerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // ✅ Read setting from CompositionLocal (provided in AppRoot)
    val settings = com.example.gymlocker.ui.settings.LocalUserSettings.current

    // ✅ Requirement:
    // - if forceDarkMode == true => always dark
    // - else => follow whatever darkTheme says (system by default)
    val effectiveDarkTheme = settings.forceDarkMode || darkTheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)

            // If we're in dark theme -> we want LIGHT icons => appearanceLightStatusBars = false
            controller.isAppearanceLightStatusBars = !effectiveDarkTheme
            controller.isAppearanceLightNavigationBars = !effectiveDarkTheme

            // Optional but recommended: avoid odd colored bars behind icons
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
        }
    }

    val colorScheme = when (settings.appTheme) {
        AppTheme.DEFAULT -> {
            // matches system theme (and optionally dynamic color)
            when {
                dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    val context = LocalContext.current
                    if (effectiveDarkTheme) dynamicDarkColorScheme(context)
                    else dynamicLightColorScheme(context)
                }
                else -> if (effectiveDarkTheme) DarkColorScheme else LightColorScheme
            }
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
        content = content
    )
}

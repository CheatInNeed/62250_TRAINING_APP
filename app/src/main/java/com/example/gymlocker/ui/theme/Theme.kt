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
private val BlueLight = lightColorScheme(primary = Color(0xFF1565C0))
private val BlueDark  = darkColorScheme(primary = Color(0xFF90CAF9))

private val GreenLight = lightColorScheme(primary = Color(0xFF2E7D32))
private val GreenDark  = darkColorScheme(primary = Color(0xFFA5D6A7))

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
    }


    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

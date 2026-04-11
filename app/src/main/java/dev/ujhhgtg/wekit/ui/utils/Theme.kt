package dev.ujhhgtg.wekit.ui.utils

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import dev.ujhhgtg.wekit.hooks.items.beautify.MonetEngine
import dev.ujhhgtg.wekit.ui.utils.theme.darkScheme
import dev.ujhhgtg.wekit.ui.utils.theme.lightScheme
import dev.ujhhgtg.wekit.utils.HostInfo

@SuppressLint("NewApi") // enforced in MonetEngine.isActive
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColors: Boolean = /*if (HostInfo.isHost) MonetEngine.isActive else false*/
        MonetEngine.isActive, // MonetEngine.isEnabled is always false in module anyways
    content: @Composable () -> Unit
) {
    MaterialExpressiveTheme(
        colorScheme = if (!dynamicColors) {
            if (darkTheme) darkScheme else lightScheme
        } else {
            if (darkTheme) dynamicDarkColorScheme(HostInfo.application)
            else dynamicLightColorScheme(HostInfo.application)
        },
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}

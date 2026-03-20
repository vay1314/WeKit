package dev.ujhhgtg.wekit.ui.utils

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import dev.ujhhgtg.wekit.ui.utils.theme.darkScheme
import dev.ujhhgtg.wekit.ui.utils.theme.lightScheme

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialExpressiveTheme(
        colorScheme = if (darkTheme) darkScheme else lightScheme,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}

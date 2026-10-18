package org.mayad.fame_life.feature.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.mayad.fame_life.app.AppConfig
import org.mayad.fame_life.core.designsystem.components.AtmosphereBackground
import org.mayad.fame_life.core.designsystem.icons.BrandMark
import org.mayad.fame_life.core.designsystem.theme.Motion
import org.mayad.fame_life.core.designsystem.theme.Pulse

/** How long the in-app splash stays at least, so it reads as a moment rather than a flicker. */
const val BRAND_SPLASH_MIN_MS = 900L

/**
 * The in-app brand moment that follows the system splash. It stays up while the saved sign-in is restored and the
 * account's data is fetched, so onboarding never flashes for someone who is already signed in.
 */
@Composable
fun BrandSplash(modifier: Modifier = Modifier) {
    val reduceMotion = Pulse.reducedMotion
    val line = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val lift = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduceMotion) return@LaunchedEffect
        launch { lift.animateTo(1f, Motion.enter()) }
        line.animateTo(1f, tween(Motion.REVEAL, easing = Motion.emphasized))
    }
    AtmosphereBackground(modifier) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandMark(
                Modifier
                    .size(112.dp)
                    .graphicsLayer {
                        val t = lift.value
                        alpha = 0.4f + 0.6f * t
                        scaleX = 0.92f + 0.08f * t
                        scaleY = scaleX
                    },
                lineProgress = line.value,
            )
            Spacer(Modifier.height(Pulse.dimens.lg))
            Text(
                AppConfig.APP_NAME,
                style = Pulse.type.headline,
                color = Pulse.colors.textPrimary,
                modifier = Modifier.graphicsLayer { alpha = lift.value },
            )
        }
    }
}

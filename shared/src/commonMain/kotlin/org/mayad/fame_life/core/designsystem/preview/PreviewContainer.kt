package org.mayad.fame_life.core.designsystem.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.designsystem.theme.PulseTheme

/**
 * Preview ground: the real theme on the app background, with motion off so previews render their final frame.
 * Arabic previews (`locale = "ar"`) switch to RTL by themselves; [rtl] forces it for English copy too.
 */
@Composable
fun PreviewContainer(
    modifier: Modifier = Modifier,
    rtl: Boolean = false,
    content: @Composable () -> Unit,
) {
    PulseTheme(reducedMotion = true) {
        val direction = if (rtl) LayoutDirection.Rtl else LocalLayoutDirection.current
        CompositionLocalProvider(LocalLayoutDirection provides direction) {
            Box(modifier.background(Pulse.colors.background)) { content() }
        }
    }
}

/** For components: default, Arabic (RTL) and large font. */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Preview(name = "Default")
@Preview(name = "Arabic", locale = "ar")
@Preview(name = "Large font", fontScale = 1.3f)
annotation class ComponentPreviews

/** For screens: phone, Arabic phone, landscape phone (split layout) and tablet. */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Preview(name = "Phone", widthDp = 393, heightDp = 852)
@Preview(name = "Phone · Arabic", widthDp = 393, heightDp = 852, locale = "ar")
@Preview(name = "Landscape", widthDp = 852, heightDp = 393)
@Preview(name = "Tablet", widthDp = 1280, heightDp = 800)
annotation class ScreenPreviews

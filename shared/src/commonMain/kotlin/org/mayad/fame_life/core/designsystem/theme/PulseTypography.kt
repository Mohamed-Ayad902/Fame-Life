package org.mayad.fame_life.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.manrope_bold
import famelife.shared.generated.resources.manrope_extrabold
import famelife.shared.generated.resources.manrope_medium
import famelife.shared.generated.resources.manrope_regular
import famelife.shared.generated.resources.manrope_semibold
import famelife.shared.generated.resources.sora_bold
import famelife.shared.generated.resources.sora_extrabold
import famelife.shared.generated.resources.sora_semibold
import org.jetbrains.compose.resources.Font

/**
 * Sora for display and numbers (geometric, confident), Manrope for reading. Arabic glyphs fall back to the
 * platform's Arabic font, which keeps RTL text native on both platforms.
 */
@Immutable
data class PulseTypography(
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val titleSmall: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val label: TextStyle,
    val meta: TextStyle,
    val metric: TextStyle,
    val metricSmall: TextStyle,
    val overline: TextStyle,
) {
    fun toMaterial() = Typography(
        displayLarge = display,
        headlineMedium = headline,
        titleLarge = title,
        titleMedium = titleSmall,
        bodyLarge = body,
        bodyMedium = body,
        bodySmall = bodySmall,
        labelLarge = label,
        labelMedium = meta,
        labelSmall = meta,
    )
}

@Composable
internal fun rememberPulseTypography(scale: PulseScale = PulseScale.Compact): PulseTypography {
    val sora = FontFamily(
        Font(Res.font.sora_semibold, FontWeight.SemiBold),
        Font(Res.font.sora_bold, FontWeight.Bold),
        Font(Res.font.sora_extrabold, FontWeight.ExtraBold),
    )
    val manrope = FontFamily(
        Font(Res.font.manrope_regular, FontWeight.Normal),
        Font(Res.font.manrope_medium, FontWeight.Medium),
        Font(Res.font.manrope_semibold, FontWeight.SemiBold),
        Font(Res.font.manrope_bold, FontWeight.Bold),
        Font(Res.font.manrope_extrabold, FontWeight.ExtraBold),
    )
    val f = scale.typeFactor
    return remember(sora, manrope, f) {
        val trim = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
        PulseTypography(
            display = TextStyle(fontFamily = sora, fontWeight = FontWeight.ExtraBold, fontSize = (34 * f).sp, lineHeight = (38 * f).sp, letterSpacing = (-0.03).em),
            headline = TextStyle(fontFamily = sora, fontWeight = FontWeight.Bold, fontSize = (24 * f).sp, lineHeight = (30 * f).sp, letterSpacing = (-0.02).em),
            title = TextStyle(fontFamily = sora, fontWeight = FontWeight.SemiBold, fontSize = (18 * f).sp, lineHeight = (24 * f).sp, letterSpacing = (-0.01).em),
            titleSmall = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Bold, fontSize = (15 * f).sp, lineHeight = (20 * f).sp),
            body = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Medium, fontSize = (15 * f).sp, lineHeight = (22 * f).sp, lineHeightStyle = trim),
            bodySmall = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Medium, fontSize = (13 * f).sp, lineHeight = (18 * f).sp),
            label = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Bold, fontSize = (14 * f).sp, lineHeight = (18 * f).sp, letterSpacing = 0.01.em),
            meta = TextStyle(fontFamily = manrope, fontWeight = FontWeight.SemiBold, fontSize = (12 * f).sp, lineHeight = (16 * f).sp),
            metric = TextStyle(fontFamily = sora, fontWeight = FontWeight.ExtraBold, fontSize = (44 * f).sp, lineHeight = (48 * f).sp, letterSpacing = (-0.04).em),
            metricSmall = TextStyle(fontFamily = sora, fontWeight = FontWeight.Bold, fontSize = (17 * f).sp, lineHeight = (20 * f).sp, letterSpacing = (-0.02).em),
            overline = TextStyle(fontFamily = manrope, fontWeight = FontWeight.ExtraBold, fontSize = (11 * f).sp, lineHeight = (14 * f).sp, letterSpacing = 0.12.em),
        )
    }
}

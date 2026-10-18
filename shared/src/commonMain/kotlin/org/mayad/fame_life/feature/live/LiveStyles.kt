package org.mayad.fame_life.feature.live

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.gift_comet
import famelife.shared.generated.resources.gift_crown
import famelife.shared.generated.resources.gift_galaxy
import famelife.shared.generated.resources.gift_heart
import famelife.shared.generated.resources.gift_mic
import famelife.shared.generated.resources.gift_spark
import famelife.shared.generated.resources.style_aurora
import famelife.shared.generated.resources.style_noir
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mayad.fame_life.core.designsystem.theme.PulseBrand
import org.mayad.fame_life.data.content.ArtMotif
import org.mayad.fame_life.data.content.ArtSpec
import org.mayad.fame_life.domain.simulation.Gift

/**
 * The look of the live overlay: an accent, a glow and the camera-off backdrop. The id is stored in
 * [org.mayad.fame_life.domain.simulation.SimulationConfig]. Colors come from the brand palette.
 */
@Immutable
data class LiveStyle(val id: String, val name: StringResource, val accent: Color, val glow: Color, val backdrop: ArtSpec)

object LiveStyles {
    val Aurora = LiveStyle("aurora", Res.string.style_aurora, PulseBrand.Violet, PulseBrand.Cyan, ArtSpec(1, ArtMotif.AURORA, 7))
    val Noir = LiveStyle("noir", Res.string.style_noir, PulseBrand.Gold, PulseBrand.Coral, ArtSpec(5, ArtMotif.STAGE, 11))
    val all = listOf(Aurora, Noir)

    fun byId(id: String) = all.firstOrNull { it.id == id } ?: Aurora
}

/** Localized gift name; unknown ids fall back to the catalog's English name. */
@Composable
fun Gift.label(): String = when (id) {
    "spark" -> stringResource(Res.string.gift_spark)
    "heart" -> stringResource(Res.string.gift_heart)
    "mic" -> stringResource(Res.string.gift_mic)
    "comet" -> stringResource(Res.string.gift_comet)
    "crown" -> stringResource(Res.string.gift_crown)
    "galaxy" -> stringResource(Res.string.gift_galaxy)
    else -> name
}

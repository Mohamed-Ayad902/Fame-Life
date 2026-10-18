package org.mayad.fame_life.feature.live

import androidx.compose.ui.graphics.Color
import org.mayad.fame_life.data.content.ArtMotif
import org.mayad.fame_life.data.content.ArtSpec
import org.mayad.fame_life.domain.simulation.AudienceLanguage
import org.mayad.fame_life.domain.simulation.AudiencePreset
import org.mayad.fame_life.domain.simulation.AudienceVibe
import org.mayad.fame_life.domain.simulation.EventFrequency

/** The look of the live overlay. The id is stored in [org.mayad.fame_life.domain.simulation.SimulationConfig]. */
data class LiveStyle(val id: String, val name: String, val accent: Color, val glow: Color, val backdrop: ArtSpec)

object LiveStyles {
    val Aurora = LiveStyle("aurora", "Aurora", Color(0xFF945CFF), Color(0xFF55E5F2), ArtSpec(1, ArtMotif.AURORA, 7))
    val Noir = LiveStyle("noir", "Noir", Color(0xFFFFC66D), Color(0xFFFF8A5B), ArtSpec(5, ArtMotif.STAGE, 11))
    val all = listOf(Aurora, Noir)

    fun byId(id: String) = all.firstOrNull { it.id == id } ?: Aurora
}

val AudiencePreset.label: String
    get() = when (this) {
        AudiencePreset.INTIMATE -> "Intimate"
        AudiencePreset.RISING -> "Rising"
        AudiencePreset.BUZZING -> "Buzzing"
        AudiencePreset.VIRAL -> "Viral"
    }

val AudiencePreset.hint: String
    get() = "Starts near ${startRange.first}, can grow past ${targetRange.first}"

val AudienceVibe.label: String
    get() = when (this) {
        AudienceVibe.SUPPORTIVE -> "Supportive"
        AudienceVibe.CURIOUS -> "Curious"
        AudienceVibe.FUNNY -> "Funny"
        AudienceVibe.HYPE -> "Hype"
        AudienceVibe.CRITICAL -> "Critics"
    }

val EventFrequency.label: String
    get() = when (this) {
        EventFrequency.CALM -> "Calm"
        EventFrequency.LIVELY -> "Lively"
        EventFrequency.WILD -> "Wild"
    }

val AudienceLanguage.label: String
    get() = when (this) {
        AudienceLanguage.ENGLISH -> "English"
        AudienceLanguage.ARABIC -> "العربية"
        AudienceLanguage.MIXED -> "Mixed"
    }

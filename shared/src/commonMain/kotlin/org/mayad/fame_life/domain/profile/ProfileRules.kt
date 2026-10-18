package org.mayad.fame_life.domain.profile

/** Why a profile field is invalid. The UI turns these into localized messages (`core/ui/Labels.kt`). */
enum class ProfileFieldError { EMPTY, TOO_SHORT, TOO_LONG, INVALID_CHARACTERS, BAD_DOTS, RESERVED }

/** Validation shared by onboarding and profile editing. Returns the problem, or null when valid. */
object ProfileRules {
    const val USERNAME_MIN = 3
    const val USERNAME_MAX = 20
    const val DISPLAY_NAME_MAX = 32
    const val BIO_MAX = 140

    private val usernamePattern = Regex("^[a-z0-9._]+$")

    /** Usernames that would let someone pose as the app or staff. */
    private val reserved = setOf("pulse", "admin", "support", "official", "staff", "moderator")

    fun normalizeUsername(input: String): String = input.trim().removePrefix("@").lowercase()

    fun usernameError(input: String): ProfileFieldError? {
        val name = normalizeUsername(input)
        return when {
            name.length < USERNAME_MIN -> ProfileFieldError.TOO_SHORT
            name.length > USERNAME_MAX -> ProfileFieldError.TOO_LONG
            !usernamePattern.matches(name) -> ProfileFieldError.INVALID_CHARACTERS
            name.startsWith(".") || name.endsWith(".") || ".." in name -> ProfileFieldError.BAD_DOTS
            name in reserved -> ProfileFieldError.RESERVED
            else -> null
        }
    }

    fun displayNameError(input: String): ProfileFieldError? {
        val name = input.trim()
        return when {
            name.isEmpty() -> ProfileFieldError.EMPTY
            name.length > DISPLAY_NAME_MAX -> ProfileFieldError.TOO_LONG
            else -> null
        }
    }
}

package org.mayad.fame_life.domain.profile

/** Validation shared by onboarding and profile editing. Returns a user-facing message, or null when valid. */
object ProfileRules {
    const val USERNAME_MIN = 3
    const val USERNAME_MAX = 20
    const val DISPLAY_NAME_MAX = 32
    const val BIO_MAX = 140

    private val usernamePattern = Regex("^[a-z0-9._]+$")

    /** Usernames that would let someone pose as the app or staff. */
    private val reserved = setOf("pulse", "admin", "support", "official", "staff", "moderator")

    fun normalizeUsername(input: String): String = input.trim().removePrefix("@").lowercase()

    fun usernameError(input: String): String? {
        val name = normalizeUsername(input)
        return when {
            name.length < USERNAME_MIN -> "Use at least $USERNAME_MIN characters"
            name.length > USERNAME_MAX -> "Use at most $USERNAME_MAX characters"
            !usernamePattern.matches(name) -> "Use letters, numbers, dots and underscores"
            name.startsWith(".") || name.endsWith(".") || ".." in name -> "Dots can't start, end or repeat"
            name in reserved -> "That username is reserved"
            else -> null
        }
    }

    fun displayNameError(input: String): String? {
        val name = input.trim()
        return when {
            name.isEmpty() -> "Add the name people will see"
            name.length > DISPLAY_NAME_MAX -> "Use at most $DISPLAY_NAME_MAX characters"
            else -> null
        }
    }
}

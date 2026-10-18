package org.mayad.fame_life.data.content

/** Procedural cover art recipe. Rendering lives in the design system; content only names the recipe. */
enum class ArtMotif { AURORA, STAGE, HORIZON, WAVES, BLOOM, PRISM, SKYLINE, PORTRAIT }

data class ArtSpec(val palette: Int, val motif: ArtMotif, val seed: Int)

enum class Category(val label: String) {
    GAMING("Gaming"),
    MUSIC("Music"),
    CHATTING("Just chatting"),
    STORYTELLING("Storytelling"),
    LIFESTYLE("Lifestyle"),
    COMEDY("Comedy"),
    ENTERTAINMENT("Entertainment"),
}

/** A simulated creator. All numbers are fictional and fixed, so every screen shows the same values. */
data class Creator(
    val id: String,
    val displayName: String,
    val handle: String,
    val category: Category,
    val bio: String,
    val followers: Int,
    val totalLikes: Int,
    val levelTitle: String,
    val avatar: ArtSpec,
    val cover: ArtSpec,
    val hue: Float,
)

data class Post(
    val id: String,
    val creatorId: String,
    val caption: String,
    val category: Category,
    val art: ArtSpec,
    /** Height divided by width, used by the masonry layout. */
    val aspect: Float,
    val likes: Int,
    val comments: Int,
    val shares: Int,
    val hoursAgo: Int,
    val tags: List<String>,
)

data class LiveRoom(
    val id: String,
    val creatorId: String,
    val title: String,
    val category: Category,
    val viewers: Int,
    val art: ArtSpec,
)

data class Topic(val tag: String, val posts: Int, val category: Category)

data class Challenge(val id: String, val title: String, val description: String, val goalMinutes: Int)

data class PostComment(val id: String, val author: String, val text: String, val likes: Int, val hoursAgo: Int)

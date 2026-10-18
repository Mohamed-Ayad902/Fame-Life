package org.mayad.fame_life.data.content

import kotlin.random.Random

/**
 * The simulated social universe. Everything is fixed data with stable ids, so a creator's follower count is
 * the same on Home, Discover and their profile. These are fictional people; nothing here represents real
 * accounts or real activity.
 */
object SeedContent {
    val creators: List<Creator> = listOf(
        creator("c_nova", "Nova Reyes", "novaplays", Category.GAMING, "Speedrunner by night, chaos gremlin by day. Co-op Fridays.", 482_300, 6_210_000, "Influencer", 2, ArtMotif.HORIZON, 1, 228f),
        creator("c_layla", "Layla Haddad", "layla.sings", Category.MUSIC, "Covers, originals and late-night piano. Requests open.", 1_240_000, 18_900_000, "Celebrity", 3, ArtMotif.WAVES, 2, 330f),
        creator("c_omar", "Omar Fathy", "omarlaughs", Category.COMEDY, "Everyday stories that got out of hand.", 356_700, 4_480_000, "Influencer", 1, ArtMotif.STAGE, 3, 18f),
        creator("c_mika", "Mika Tan", "mika.cooks", Category.LIFESTYLE, "Twenty-minute dinners and one ambitious dessert a week.", 214_900, 2_330_000, "Known Creator", 4, ArtMotif.BLOOM, 4, 140f),
        creator("c_zain", "Zain Al-Masri", "zain.talks", Category.CHATTING, "Talk shows from my kitchen table. Bring tea.", 98_400, 870_000, "Known Creator", 0, ArtMotif.AURORA, 5, 200f),
        creator("c_ivy", "Ivy Calloway", "ivytells", Category.STORYTELLING, "True-ish stories. You decide which parts.", 667_100, 9_050_000, "Influencer", 5, ArtMotif.SKYLINE, 6, 268f),
        creator("c_dex", "Dex Okafor", "dexbeats", Category.MUSIC, "Making a beat from whatever you comment.", 529_800, 7_120_000, "Influencer", 6, ArtMotif.WAVES, 7, 172f),
        creator("c_sara", "Sara Mansour", "sara.fit", Category.LIFESTYLE, "Strong, not perfect. Morning lives at 7.", 143_200, 1_610_000, "Known Creator", 7, ArtMotif.PRISM, 8, 156f),
        creator("c_jun", "Jun Park", "junspeedruns", Category.GAMING, "Frame-perfect or bust.", 76_500, 640_000, "Rising Creator", 0, ArtMotif.HORIZON, 9, 210f),
        creator("c_rania", "رانيا خليل", "rania.k", Category.CHATTING, "سوالف آخر الليل وقصص من القاهرة ☕️", 311_000, 3_900_000, "Influencer", 3, ArtMotif.AURORA, 10, 300f),
        creator("c_theo", "Theo Marsh", "theo.after.dark", Category.ENTERTAINMENT, "Movie nights, hot takes, cold pizza.", 189_300, 2_050_000, "Known Creator", 1, ArtMotif.SKYLINE, 11, 245f),
        creator("c_amira", "Amira Nasser", "amira.travels", Category.LIFESTYLE, "One carry-on, forty countries.", 902_600, 12_400_000, "Celebrity", 4, ArtMotif.PRISM, 12, 36f),
        creator("c_kofi", "Kofi Mensah", "kofi.jokes", Category.COMEDY, "Crowd work, but the crowd is my group chat.", 254_700, 3_120_000, "Influencer", 2, ArtMotif.STAGE, 13, 48f),
        creator("c_luna", "Luna Vega", "lunavega", Category.ENTERTAINMENT, "Reacting to things so you don't have to.", 1_870_000, 26_300_000, "Superstar", 5, ArtMotif.BLOOM, 14, 285f),
    )

    private val captionsByCategory = mapOf(
        Category.GAMING to listOf(
            "New personal best and I'm still shaking", "The boss that took 47 tries", "Co-op night got way out of hand",
            "Rating every level from worst to best", "This glitch should not exist",
        ),
        Category.MUSIC to listOf(
            "Wrote this at 3am, be gentle", "Your requests, my piano", "Golden hour session on the roof",
            "Turned your comments into a beat", "First time singing this one live",
        ),
        Category.COMEDY to listOf(
            "My mom found my channel", "Ranking the worst advice I've ever gotten", "The wedding story, finally",
            "I asked my group chat to roast me", "Airport security vs. me",
        ),
        Category.LIFESTYLE to listOf(
            "Twenty-minute dinner that tastes like two hours", "Morning routine, the honest version", "Packing for 3 weeks in one bag",
            "Sunday reset", "Tiny balcony garden update",
        ),
        Category.CHATTING to listOf(
            "Unpopular opinions, round two", "Tea and questions you sent me", "What I'd tell my 18-year-old self",
            "Late night chat about everything", "سوالف وقهوة",
        ),
        Category.STORYTELLING to listOf(
            "The night the lights went out in the hostel", "A stranger on the train told me this", "Part 3: the letter",
            "The house at the end of my street", "I still can't explain this one",
        ),
        Category.ENTERTAINMENT to listOf(
            "Reacting to the most chaotic trailer of the year", "Movies that deserved a sequel", "Ranking every finale",
            "Watching it so you don't have to", "Plot twist tier list",
        ),
    )

    private val tagsByCategory = mapOf(
        Category.GAMING to listOf("speedrun", "coop", "gg"),
        Category.MUSIC to listOf("cover", "original", "piano"),
        Category.COMEDY to listOf("storytime", "roast", "funny"),
        Category.LIFESTYLE to listOf("routine", "food", "travel"),
        Category.CHATTING to listOf("talkshow", "questions", "latenight"),
        Category.STORYTELLING to listOf("storytime", "mystery", "series"),
        Category.ENTERTAINMENT to listOf("reaction", "movies", "tierlist"),
    )

    val posts: List<Post> = buildList {
        creators.forEachIndexed { creatorIndex, creator ->
            val random = Random(stableHash(creator.id))
            val captions = captionsByCategory.getValue(creator.category)
            val count = 3 + creatorIndex % 2
            repeat(count) { i ->
                val motifs = ArtMotif.entries.filter { it != ArtMotif.PORTRAIT }
                add(
                    Post(
                        id = "${creator.id}_p$i",
                        creatorId = creator.id,
                        caption = captions[(i + creatorIndex) % captions.size],
                        category = creator.category,
                        art = ArtSpec(palette = (creator.avatar.palette + i) % 8, motif = motifs[(creatorIndex + i * 3) % motifs.size], seed = creatorIndex * 10 + i),
                        aspect = listOf(1.25f, 1.6f, 1.0f, 1.4f)[(creatorIndex + i) % 4],
                        likes = (creator.followers * random.nextDouble(0.02, 0.11)).toInt(),
                        comments = (creator.followers * random.nextDouble(0.002, 0.012)).toInt(),
                        shares = (creator.followers * random.nextDouble(0.001, 0.006)).toInt(),
                        hoursAgo = 1 + (creatorIndex * 7 + i * 13) % 96,
                        tags = tagsByCategory.getValue(creator.category).take(2 + i % 2),
                    ),
                )
            }
        }
    }.sortedBy { it.hoursAgo }

    val liveRooms: List<LiveRoom> = listOf(
        live("l1", "c_layla", "Piano requests till midnight", 18_400, 0),
        live("l2", "c_nova", "Any% attempts, no sleep", 6_920, 1),
        live("l3", "c_rania", "سهرة سوالف ☕️", 9_310, 2),
        live("l4", "c_kofi", "Roasting your group chats", 4_180, 3),
        live("l5", "c_amira", "Live from a night market", 12_760, 4),
        live("l6", "c_dex", "Beat from your comments", 7_540, 5),
        live("l7", "c_ivy", "Campfire stories", 3_090, 6),
    )

    val topics: List<Topic> = listOf(
        Topic("latenightchat", 48_200, Category.CHATTING),
        Topic("speedrun", 31_700, Category.GAMING),
        Topic("pianocovers", 27_900, Category.MUSIC),
        Topic("storytime", 64_100, Category.STORYTELLING),
        Topic("20mindinner", 19_400, Category.LIFESTYLE),
        Topic("roastme", 22_800, Category.COMEDY),
        Topic("tierlist", 15_300, Category.ENTERTAINMENT),
        Topic("سوالف", 12_600, Category.CHATTING),
    )

    val challenges: List<Challenge> = listOf(
        Challenge("ch_story", "Tell a story in 5 minutes", "Go live and share a story with a twist. Your audience decides how it ends.", 5),
        Challenge("ch_qa", "Ten-minute Q&A", "Let the audience ask anything and answer as many as you can.", 10),
        Challenge("ch_hot", "Defend a hot take", "Pick an unpopular opinion and win the chat over.", 5),
    )

    fun creator(id: String): Creator? = creators.firstOrNull { it.id == id }
    fun post(id: String): Post? = posts.firstOrNull { it.id == id }
    fun postsBy(creatorId: String): List<Post> = posts.filter { it.creatorId == creatorId }
    fun liveFor(creatorId: String): LiveRoom? = liveRooms.firstOrNull { it.creatorId == creatorId }

    /** Comments on a post are generated from the post id, so they are identical every time the post opens. */
    fun commentsFor(post: Post): List<PostComment> {
        val random = Random(stableHash(post.id))
        val count = 6 + random.nextInt(5)
        return List(count) { i ->
            val author = commentAuthors[random.nextInt(commentAuthors.size)]
            PostComment(
                id = "${post.id}_c$i",
                author = author,
                text = commentLines[random.nextInt(commentLines.size)],
                likes = (post.likes * random.nextDouble(0.0005, 0.01)).toInt(),
                hoursAgo = (post.hoursAgo - random.nextInt(post.hoursAgo + 1)).coerceAtLeast(0),
            )
        }.distinctBy { it.text }.sortedByDescending { it.likes }
    }

    fun search(query: String): SearchResults {
        val q = query.trim().lowercase().removePrefix("#").removePrefix("@")
        if (q.isEmpty()) return SearchResults(emptyList(), emptyList(), emptyList())
        return SearchResults(
            creators = creators.filter { q in it.displayName.lowercase() || q in it.handle || q in it.category.label.lowercase() },
            posts = posts.filter { p -> q in p.caption.lowercase() || p.tags.any { q in it } || q in p.category.label.lowercase() },
            topics = topics.filter { q in it.tag },
        )
    }

    /** Hash that is identical on every platform, unlike String.hashCode guarantees. */
    fun stableHash(value: String): Int = value.fold(17) { acc, c -> acc * 31 + c.code }

    private val commentAuthors = listOf(
        "maya.live", "jordan_vibes", "leo99", "sky.fm", "noura_daily", "omar.k", "kai_on_air", "iris.wav",
        "salma777", "theo_x", "hala__", "finn.live", "luna_daily", "zaid.fm",
    )
    private val commentLines = listOf(
        "this is exactly what I needed today", "the ending got me 😭", "how is this so good", "part two please!!",
        "watched this three times already", "the editing on this 🔥", "ok this deserves more views",
        "I laughed way too loud at this", "saving this for later", "you never miss", "مبدع كالعادة 👏",
        "who else is here at 2am", "the vibe is unmatched", "sending this to my sister rn",
    )

    private fun creator(
        id: String, name: String, handle: String, category: Category, bio: String, followers: Int, likes: Int,
        level: String, palette: Int, coverMotif: ArtMotif, seed: Int, hue: Float,
    ) = Creator(
        id = id,
        displayName = name,
        handle = handle,
        category = category,
        bio = bio,
        followers = followers,
        totalLikes = likes,
        levelTitle = level,
        avatar = ArtSpec(palette, ArtMotif.PORTRAIT, seed),
        cover = ArtSpec(palette, coverMotif, seed + 100),
        hue = hue,
    )

    private fun live(id: String, creatorId: String, title: String, viewers: Int, seed: Int): LiveRoom {
        val creator = creators.first { it.id == creatorId }
        return LiveRoom(id, creatorId, title, creator.category, viewers, ArtSpec(creator.avatar.palette, ArtMotif.PORTRAIT, 200 + seed))
    }
}

data class SearchResults(val creators: List<Creator>, val posts: List<Post>, val topics: List<Topic>) {
    val isEmpty: Boolean get() = creators.isEmpty() && posts.isEmpty() && topics.isEmpty()
}

package org.mayad.fame_life.domain.ai

/** Coarse topic families the on-device fallback can recognize. The LLM path does not depend on these. */
enum class TopicFamily { GAMING, MUSIC, FOOD, SPORTS, TRAVEL, FITNESS, WORK, NONE }

/**
 * Lightweight, on-device reading of one utterance in English or Arabic. It only powers the fallback audience
 * and request triggers, so it favours cheap, predictable heuristics over accuracy.
 */
data class SpeechSignals(
    val isQuestion: Boolean,
    val isGreeting: Boolean,
    val isFunny: Boolean,
    val isPositive: Boolean,
    val isNegative: Boolean,
    val isHype: Boolean,
    val topic: TopicFamily,
    val keyword: String?,
    val isArabic: Boolean,
) {
    companion object {
        fun of(text: String): SpeechSignals {
            val lower = text.lowercase()
            val words = lower.split(Regex("[^\\p{L}\\p{N}']+")).filter { it.isNotBlank() }
            val arabic = text.count { it in '؀'..'ۿ' } > text.count { it in 'a'..'z' || it in 'A'..'Z' }
            return SpeechSignals(
                isQuestion = '?' in text || '؟' in text || words.take(3).any { it in questionWords } ||
                    questionPhrases.any { lower.startsWith(it) },
                isGreeting = words.take(4).any { it in greetingWords } || greetingPhrases.any { it in lower },
                isFunny = funnyWords.any { it in lower } || "😂" in text,
                isPositive = words.any { it in positiveWords },
                isNegative = words.any { it in negativeWords },
                isHype = words.any { it in hypeWords } || text.count { it == '!' } >= 2,
                topic = topicOf(words),
                keyword = keywordOf(words),
                isArabic = arabic,
            )
        }

        private fun topicOf(words: List<String>): TopicFamily =
            topics.entries.firstOrNull { (_, keys) -> words.any { word -> keys.any { word.startsWith(it) } } }?.key
                ?: TopicFamily.NONE

        /** The most specific word in the sentence: the longest one that is not a stop word. */
        private fun keywordOf(words: List<String>): String? = words
            .filter { it.length >= 4 && it !in stopWords && it.none(Char::isDigit) }
            .maxByOrNull { it.length }

        private val questionWords = setOf(
            "what", "how", "why", "who", "where", "when", "which", "should",
            "هل", "ليش", "ليه", "كيف", "شو", "ايش", "وش", "متى", "مين", "ماذا", "لماذا", "اي",
        )
        private val questionPhrases = listOf("do you", "can you", "would you", "have you", "are you", "is it", "did you")
        private val greetingWords = setOf("hi", "hello", "hey", "welcome", "yo", "مرحبا", "اهلا", "أهلا", "هلا", "السلام")
        private val greetingPhrases = listOf("good morning", "good evening", "صباح الخير", "مساء الخير", "السلام عليكم")
        private val funnyWords = listOf("haha", "lol", "lmao", "joke", "funny", "hilarious", "ههه", "نكتة", "تضحك")
        private val positiveWords = setOf(
            "love", "happy", "excited", "amazing", "great", "best", "won", "win", "awesome", "proud", "beautiful",
            "احب", "أحب", "سعيد", "فرحان", "رائع", "حلو", "جميل", "مبسوط", "فزت",
        )
        private val negativeWords = setOf(
            "sad", "tired", "hard", "miss", "lost", "stress", "stressed", "failed", "hate", "lonely", "sick",
            "زعلان", "تعبان", "حزين", "صعب", "خسرت", "مضغوط",
        )
        private val hypeWords = setOf("lets", "insane", "crazy", "hype", "fire", "epic", "يلا", "نار", "خرافي")
        private val topics = mapOf(
            TopicFamily.GAMING to listOf("game", "gaming", "play", "level", "boss", "ps5", "xbox", "fortnite", "minecraft", "valorant", "لعب", "لعبة"),
            TopicFamily.MUSIC to listOf("song", "sing", "music", "beat", "guitar", "album", "rap", "اغني", "أغني", "موسيق"),
            TopicFamily.FOOD to listOf("food", "eat", "cook", "pizza", "coffee", "dinner", "lunch", "breakfast", "اكل", "أكل", "قهوة", "طبخ"),
            TopicFamily.SPORTS to listOf("match", "goal", "football", "soccer", "team", "league", "كورة", "مباراة", "هدف"),
            TopicFamily.TRAVEL to listOf("travel", "trip", "flight", "beach", "city", "سفر", "رحلة"),
            TopicFamily.FITNESS to listOf("gym", "workout", "run", "training", "diet", "جيم", "تمرين"),
            TopicFamily.WORK to listOf("work", "job", "study", "exam", "boss", "university", "school", "شغل", "دراسة", "امتحان", "جامعة"),
        )
        private val stopWords = setOf(
            "this", "that", "with", "have", "just", "like", "really", "about", "there", "they", "what", "your",
            "from", "been", "were", "then", "than", "them", "when", "will", "would", "could", "should", "think",
            "know", "today", "going", "gonna", "want", "because", "actually", "literally", "something", "anyone",
            "everyone", "guys", "yeah", "okay", "right", "where", "which", "maybe", "very", "much", "some",
            "انا", "أنا", "هذا", "هذه", "اللي", "يعني", "عشان", "لأن", "كان", "كانت", "عندي", "والله", "شوي", "كتير", "كثير",
        )
    }
}

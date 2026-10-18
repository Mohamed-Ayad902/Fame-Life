package org.mayad.fame_life.core.format

import org.mayad.fame_life.data.content.SeedContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FormattersTest {
    @Test
    fun compactCounts() {
        assertEquals("870", compactCount(870))
        assertEquals("1K", compactCount(1_000))
        assertEquals("24.5K", compactCount(24_480))
        assertEquals("482K", compactCount(482_300))
        assertEquals("1M", compactCount(999_960))
        assertEquals("1.2M", compactCount(1_240_000))
        assertEquals("26.3M", compactCount(26_300_000))
    }

    @Test
    fun clocks() {
        assertEquals("00:07", clock(7))
        assertEquals("12:30", clock(750))
        assertEquals("1:02:03", clock(3723))
    }
}

class SeedContentTest {
    @Test
    fun idsAreUniqueAndReferencesResolve() {
        assertEquals(SeedContent.creators.size, SeedContent.creators.map { it.id }.toSet().size)
        assertEquals(SeedContent.posts.size, SeedContent.posts.map { it.id }.toSet().size)
        assertTrue(SeedContent.posts.all { SeedContent.creator(it.creatorId) != null })
        assertTrue(SeedContent.liveRooms.all { SeedContent.creator(it.creatorId) != null })
    }

    @Test
    fun generatedValuesAreStableAcrossCalls() {
        val post = SeedContent.posts.first()
        assertEquals(SeedContent.commentsFor(post), SeedContent.commentsFor(post))
        assertEquals(SeedContent.stableHash("c_nova"), SeedContent.stableHash("c_nova"))
    }

    @Test
    fun searchMatchesHandlesTagsAndCategories() {
        assertTrue(SeedContent.search("@novaplays").creators.any { it.id == "c_nova" })
        assertTrue(SeedContent.search("#storytime").topics.isNotEmpty())
        assertTrue(SeedContent.search("music").posts.isNotEmpty())
        assertTrue(SeedContent.search("zzzz-nothing").isEmpty)
    }
}

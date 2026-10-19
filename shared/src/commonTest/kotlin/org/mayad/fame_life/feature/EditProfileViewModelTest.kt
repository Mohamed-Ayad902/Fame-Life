package org.mayad.fame_life.feature

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.mayad.fame_life.core.platform.InMemoryTextStore
import org.mayad.fame_life.data.local.CreatorProfile
import org.mayad.fame_life.data.local.ProfileRepository
import org.mayad.fame_life.feature.profile.EditProfileEvent
import org.mayad.fame_life.feature.profile.EditProfileIntent
import org.mayad.fame_life.feature.profile.EditProfileViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditProfileViewModelTest : ViewModelTest() {
    private val profiles = ProfileRepository(InMemoryTextStore()) { _, _ -> }.apply {
        save(CreatorProfile(displayName = "Mo", username = "maya_live", createdAtEpochMs = 1))
    }

    @Test
    fun saveIsOnlyPossibleForAValidChange() {
        val vm = EditProfileViewModel(profiles)
        assertFalse(vm.state.value.canSave, "nothing changed yet")
        vm.onIntent(EditProfileIntent.Name(""))
        assertFalse(vm.state.value.canSave, "an empty name is invalid")
        vm.onIntent(EditProfileIntent.Name("Mona"))
        assertTrue(vm.state.value.canSave)
    }

    @Test
    fun savingWritesTheTrimmedProfileAndFinishes() = runTest {
        val vm = EditProfileViewModel(profiles)
        vm.onIntent(EditProfileIntent.Name("  Mona "))
        vm.onIntent(EditProfileIntent.Bio(" hi "))
        vm.onIntent(EditProfileIntent.Save)
        assertEquals(EditProfileEvent.Saved, vm.events.first())
        assertEquals("Mona", profiles.profile.value?.displayName)
        assertEquals("hi", profiles.profile.value?.bio)
    }
}

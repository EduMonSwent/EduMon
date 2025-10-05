package com.android.sample.ui

import com.android.sample.ui.profile.ProfileViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setup() {
        viewModel = ProfileViewModel()
    }

    @Test
    fun initialStateHasDefaultUserProfileValues() = runTest {
        val user = viewModel.userProfile.value
        Assert.assertEquals("Alex", user.name)
        Assert.assertEquals("alex@university.edu", user.email)
        Assert.assertEquals(5, user.level)
        Assert.assertEquals(1250, user.points)
        Assert.assertEquals(true, user.notificationsEnabled)
        Assert.assertEquals(true, user.locationEnabled)
        Assert.assertEquals(false, user.focusModeEnabled)
    }

    @Test
    fun toggleNotificationsInvertsCurrentState() = runTest {
        val initial = viewModel.userProfile.value.notificationsEnabled
        viewModel.toggleNotifications()
        val updated = viewModel.userProfile.value.notificationsEnabled
        Assert.assertEquals(!initial, updated)
    }

    @Test
    fun toggleLocationInvertsCurrentState() = runTest {
        val initial = viewModel.userProfile.value.locationEnabled
        viewModel.toggleLocation()
        val updated = viewModel.userProfile.value.locationEnabled
        Assert.assertEquals(!initial, updated)
    }

    @Test
    fun toggleFocusModeInvertsCurrentState() = runTest {
        val initial = viewModel.userProfile.value.focusModeEnabled
        viewModel.toggleFocusMode()
        val updated = viewModel.userProfile.value.focusModeEnabled
        Assert.assertEquals(!initial, updated)
    }

    @Test
    fun multipleTogglesMaintainConsistentUserState() = runTest {
        val initial = viewModel.userProfile.value

        viewModel.toggleNotifications()
        viewModel.toggleLocation()
        viewModel.toggleFocusMode()

        val updated = viewModel.userProfile.value


        Assert.assertEquals(!initial.notificationsEnabled, updated.notificationsEnabled)
        Assert.assertEquals(!initial.locationEnabled, updated.locationEnabled)
        Assert.assertEquals(!initial.focusModeEnabled, updated.focusModeEnabled)
    }
}
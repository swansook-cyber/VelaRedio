package net.velalab.radio

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VersionDisplayTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test fun sidebarVersionMatchesInstalledPackage() {
        val activity = composeRule.activity
        val version = activity.packageManager.getPackageInfo(activity.packageName, 0).versionName
        require(!version.isNullOrBlank())
        composeRule.onNodeWithTag("installed-version").assertTextEquals("V$version")
    }
}

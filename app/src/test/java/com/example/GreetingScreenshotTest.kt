package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.Event
import com.example.data.EventType
import com.example.ui.screens.EventCardRow
import com.example.ui.theme.MyApplicationTheme
import com.example.util.CalculationSettings
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val mockEvent = Event(
        id = 1L,
        title = "Paris Vacation",
        targetDate = "2026-12-25",
        targetTime = "12:00",
        eventType = EventType.COUNTDOWN,
        themeColorHex = "#EC4899",
        customLocalIconId = "alarm",
        groupId = null,
        manualSortOrder = 0,
        eventNote = "A dreamy trip to Paris!",
        localImageUri = null
    )

    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = true) {
        EventCardRow(
            event = mockEvent,
            settings = CalculationSettings(),
            isSortEditMode = false,
            isSelected = false,
            manualSortActive = false,
            onCardClick = {},
            onEditClick = {},
            onMoveUp = {},
            onMoveDown = {},
            onSelectToggle = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

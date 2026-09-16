package com.example

import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.DeckEntity
import com.example.domain.srs.CardState
import com.example.ui.cards.CardRowItem
import com.example.ui.theme.MyApplicationTheme
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
class CardListScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun cardRowItem_screenshot() {
        val sampleCard = CardEntity(
            id = 1,
            deckId = 1,
            front = "What is the SM-2 algorithm?",
            back = "A spaced repetition calculation method developed by Piotr Woźniak to calculate optimal memory intervals.",
            state = CardState.REVIEW,
            intervalDays = 6,
            easeFactor = 2.6,
            repetitions = 3
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                Surface {
                    CardRowItem(
                        card = sampleCard,
                        onEditClick = {},
                        onDeleteClick = {}
                    )
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/card_row_item.png")
    }
}

package io.github.ardaulas.marque.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.ardaulas.marque.R
import io.github.ardaulas.marque.core.result.DataError
import io.github.ardaulas.marque.domain.model.Make
import io.github.ardaulas.marque.domain.model.Model
import io.github.ardaulas.marque.ui.makes.MakesScreen
import io.github.ardaulas.marque.ui.makes.MakesUiState
import io.github.ardaulas.marque.ui.models.ModelsScreen
import io.github.ardaulas.marque.ui.models.ModelsUiState
import io.github.ardaulas.marque.ui.theme.MarqueTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Drives the stateless screens directly; no ViewModel, Hilt, or navigation involved. */
@RunWith(AndroidJUnit4::class)
class ListScreensTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val makes = listOf(Make(440, "ASTON MARTIN"), Make(441, "TESLA"), Make(448, "TOYOTA"))

    private fun string(
        id: Int,
        vararg args: Any,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    @Test
    fun makesContent_rendersRows_andClickReportsTheMakeId() {
        val clicked = mutableListOf<Int>()
        composeTestRule.setContent {
            MarqueTheme {
                MakesScreen(
                    uiState = MakesUiState.Content(makes, query = "", isRefreshing = false, error = null),
                    onQueryChange = {},
                    onRefresh = {},
                    onMakeClick = { clicked += it },
                )
            }
        }

        composeTestRule.onNodeWithText("ASTON MARTIN").assertIsDisplayed()
        composeTestRule.onNodeWithText("TOYOTA").assertIsDisplayed()
        composeTestRule.onNodeWithText("TESLA").performClick()

        assertEquals(listOf(441), clicked)
    }

    @Test
    fun makesError_showsTheMessage_andRetryInvokesTheCallback() {
        var retries = 0
        composeTestRule.setContent {
            MarqueTheme {
                MakesScreen(
                    uiState = MakesUiState.Error(DataError.Http(503)),
                    onQueryChange = {},
                    onRefresh = { retries++ },
                    onMakeClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.error_http, 503)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.action_retry)).performClick()

        assertEquals(1, retries)
    }

    @Test
    fun makesContentWithError_showsCachedRowsAndTheBanner() {
        composeTestRule.setContent {
            MarqueTheme {
                MakesScreen(
                    uiState = MakesUiState.Content(makes, query = "", isRefreshing = false, error = DataError.Network),
                    onQueryChange = {},
                    onRefresh = {},
                    onMakeClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("ASTON MARTIN").assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.showing_saved_data, string(R.string.error_network)))
            .assertIsDisplayed()
    }

    @Test
    fun makesContent_withNoMatch_showsTheNoMatchMessage() {
        composeTestRule.setContent {
            MarqueTheme {
                MakesScreen(
                    uiState = MakesUiState.Content(emptyList(), query = "zzz", isRefreshing = false, error = null),
                    onQueryChange = {},
                    onRefresh = {},
                    onMakeClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.makes_no_match)).assertIsDisplayed()
    }

    @Test
    fun modelsContentWithError_showsTitleRowsBanner_andBackInvokesTheCallback() {
        var backs = 0
        composeTestRule.setContent {
            MarqueTheme {
                ModelsScreen(
                    uiState =
                        ModelsUiState.Content(
                            makeName = "TOYOTA",
                            models = listOf(Model(2208, "Corolla")),
                            isRefreshing = false,
                            error = DataError.Timeout,
                        ),
                    onRefresh = {},
                    onBack = { backs++ },
                )
            }
        }

        composeTestRule.onNodeWithText("TOYOTA").assertIsDisplayed()
        composeTestRule.onNodeWithText("Corolla").assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.showing_saved_data, string(R.string.error_timeout)))
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(string(R.string.action_back)).performClick()

        assertEquals(1, backs)
    }
}

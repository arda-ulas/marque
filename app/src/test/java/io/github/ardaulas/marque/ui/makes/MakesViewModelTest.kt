package io.github.ardaulas.marque.ui.makes

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.github.ardaulas.marque.core.result.DataError
import io.github.ardaulas.marque.core.result.Result
import io.github.ardaulas.marque.domain.model.Make
import io.github.ardaulas.marque.ui.FakeVehicleRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MakesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeVehicleRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) = MakesViewModel(repository, savedStateHandle)

    @Test
    fun `starts in Loading and shows Content once the refresh succeeds`() =
        runTest(dispatcher) {
            repository.refreshResult = Result.Success(Unit)
            repository.makesToPublishOnRefresh = listOf(Make(440, "ASTON MARTIN"))

            val viewModel = viewModel()

            viewModel.uiState.test {
                assertEquals(MakesUiState.Loading, awaitItem())
                advanceUntilIdle()
                assertEquals(
                    MakesUiState.Content(listOf(Make(440, "ASTON MARTIN")), query = "", isRefreshing = false, error = null),
                    expectMostRecentItem(),
                )
            }
        }

    @Test
    fun `cached data plus a failing refresh is Content with the error, not Error`() =
        runTest(dispatcher) {
            repository.makes.value = listOf(Make(440, "ASTON MARTIN"))
            repository.refreshResult = Result.Failure(DataError.Network)

            val viewModel = viewModel()

            viewModel.uiState.test {
                assertEquals(MakesUiState.Loading, awaitItem())
                advanceUntilIdle()
                assertEquals(
                    MakesUiState.Content(
                        listOf(Make(440, "ASTON MARTIN")),
                        query = "",
                        isRefreshing = false,
                        error = DataError.Network,
                    ),
                    expectMostRecentItem(),
                )
            }
        }

    @Test
    fun `no cache plus a failing refresh is Error`() =
        runTest(dispatcher) {
            repository.refreshResult = Result.Failure(DataError.Timeout)

            val viewModel = viewModel()

            viewModel.uiState.test {
                assertEquals(MakesUiState.Loading, awaitItem())
                advanceUntilIdle()
                assertEquals(MakesUiState.Error(DataError.Timeout), expectMostRecentItem())
            }
        }

    @Test
    fun `no cache and a successful but empty refresh is Empty`() =
        runTest(dispatcher) {
            repository.refreshResult = Result.Success(Unit)

            val viewModel = viewModel()

            viewModel.uiState.test {
                assertEquals(MakesUiState.Loading, awaitItem())
                advanceUntilIdle()
                assertEquals(MakesUiState.Empty, expectMostRecentItem())
            }
        }

    @Test
    fun `the initial refresh is not forced but a user retry is`() =
        runTest(dispatcher) {
            repository.refreshResult = Result.Failure(DataError.Network)
            val viewModel = viewModel()

            viewModel.uiState.test {
                advanceUntilIdle()
                assertEquals(listOf(false), repository.refreshForceCalls)

                repository.refreshResult = Result.Success(Unit)
                repository.makesToPublishOnRefresh = listOf(Make(441, "TESLA"))
                viewModel.refresh()
                advanceUntilIdle()

                assertEquals(listOf(false, true), repository.refreshForceCalls)
                assertEquals(
                    MakesUiState.Content(listOf(Make(441, "TESLA")), query = "", isRefreshing = false, error = null),
                    expectMostRecentItem(),
                )
            }
        }

    @Test
    fun `a refresh over cached data reports isRefreshing while in flight`() =
        runTest(dispatcher) {
            repository.makes.value = listOf(Make(440, "ASTON MARTIN"))
            repository.refreshResult = Result.Success(Unit)
            val gate = CompletableDeferred<Unit>()
            repository.refreshGate = gate
            val viewModel = viewModel()

            viewModel.uiState.test {
                assertEquals(MakesUiState.Loading, awaitItem())
                // The refresh is parked on the gate, so the combine emits the in-flight state.
                runCurrent()
                val inFlight = awaitItem() as MakesUiState.Content
                assertTrue(inFlight.isRefreshing)

                gate.complete(Unit)
                advanceUntilIdle()
                val settled = expectMostRecentItem() as MakesUiState.Content
                assertFalse(settled.isRefreshing)
            }
        }

    @Test
    fun `the query filters case-insensitively and ignores surrounding whitespace`() =
        runTest(dispatcher) {
            repository.makes.value = listOf(Make(440, "ASTON MARTIN"), Make(441, "TESLA"), Make(448, "TOYOTA"))
            val viewModel = viewModel()

            viewModel.uiState.test {
                advanceUntilIdle()
                viewModel.setQuery("  to ")
                advanceUntilIdle()

                assertEquals(
                    MakesUiState.Content(
                        listOf(Make(440, "ASTON MARTIN"), Make(448, "TOYOTA")),
                        query = "  to ",
                        isRefreshing = false,
                        error = null,
                    ),
                    expectMostRecentItem(),
                )
            }
        }

    @Test
    fun `a query matching nothing is Content with an empty list, not Empty`() =
        runTest(dispatcher) {
            repository.makes.value = listOf(Make(441, "TESLA"))
            val viewModel = viewModel()

            viewModel.uiState.test {
                advanceUntilIdle()
                viewModel.setQuery("zzz")
                advanceUntilIdle()

                assertEquals(
                    MakesUiState.Content(emptyList(), query = "zzz", isRefreshing = false, error = null),
                    expectMostRecentItem(),
                )
            }
        }

    @Test
    fun `a query restored from saved state is applied before the first Content`() =
        runTest(dispatcher) {
            repository.makes.value = listOf(Make(441, "TESLA"), Make(448, "TOYOTA"))
            val restored = SavedStateHandle(mapOf(MakesViewModel.QUERY_KEY to "tes"))
            val viewModel = viewModel(restored)

            viewModel.uiState.test {
                advanceUntilIdle()
                assertEquals(
                    MakesUiState.Content(listOf(Make(441, "TESLA")), query = "tes", isRefreshing = false, error = null),
                    expectMostRecentItem(),
                )
            }
        }

    @Test
    fun `setQuery writes through to the SavedStateHandle`() =
        runTest(dispatcher) {
            val handle = SavedStateHandle()
            val viewModel = viewModel(handle)

            viewModel.setQuery("bmw")

            assertEquals("bmw", handle.get<String>(MakesViewModel.QUERY_KEY))
        }
}

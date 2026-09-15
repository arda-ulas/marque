package io.github.ardaulas.marque.ui.makes

import app.cash.turbine.test
import io.github.ardaulas.marque.core.result.DataError
import io.github.ardaulas.marque.core.result.Result
import io.github.ardaulas.marque.data.repository.VehicleRepository
import io.github.ardaulas.marque.domain.model.Make
import io.github.ardaulas.marque.domain.model.Model
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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

    @Test
    fun `starts in Loading and shows Content once the refresh succeeds`() =
        runTest(dispatcher) {
            repository.refreshResult = Result.Success(Unit)
            repository.makesToPublishOnRefresh = listOf(Make(440, "ASTON MARTIN"))

            val viewModel = MakesViewModel(repository)

            viewModel.uiState.test {
                assertEquals(MakesUiState.Loading, awaitItem())
                advanceUntilIdle()
                assertEquals(
                    MakesUiState.Content(listOf(Make(440, "ASTON MARTIN")), isRefreshing = false, error = null),
                    expectMostRecentItem(),
                )
            }
        }

    @Test
    fun `cached data plus a failing refresh is Content with the error, not Error`() =
        runTest(dispatcher) {
            repository.makes.value = listOf(Make(440, "ASTON MARTIN"))
            repository.refreshResult = Result.Failure(DataError.Network)

            val viewModel = MakesViewModel(repository)

            viewModel.uiState.test {
                assertEquals(MakesUiState.Loading, awaitItem())
                advanceUntilIdle()
                assertEquals(
                    MakesUiState.Content(listOf(Make(440, "ASTON MARTIN")), isRefreshing = false, error = DataError.Network),
                    expectMostRecentItem(),
                )
            }
        }

    @Test
    fun `no cache plus a failing refresh is Error`() =
        runTest(dispatcher) {
            repository.refreshResult = Result.Failure(DataError.Timeout)

            val viewModel = MakesViewModel(repository)

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

            val viewModel = MakesViewModel(repository)

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
            val viewModel = MakesViewModel(repository)

            viewModel.uiState.test {
                advanceUntilIdle()
                assertEquals(listOf(false), repository.refreshForceCalls)

                repository.refreshResult = Result.Success(Unit)
                repository.makesToPublishOnRefresh = listOf(Make(441, "TESLA"))
                viewModel.refresh()
                advanceUntilIdle()

                assertEquals(listOf(false, true), repository.refreshForceCalls)
                assertEquals(
                    MakesUiState.Content(listOf(Make(441, "TESLA")), isRefreshing = false, error = null),
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
            val viewModel = MakesViewModel(repository)

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
}

private class FakeVehicleRepository : VehicleRepository {
    val makes = MutableStateFlow<List<Make>>(emptyList())
    var refreshResult: Result<Unit, DataError> = Result.Success(Unit)

    /** Simulates the network writing the cache: published into [makes] when a refresh succeeds. */
    var makesToPublishOnRefresh: List<Make>? = null
    val refreshForceCalls = mutableListOf<Boolean>()

    /** When set, [refreshMakes] suspends until it completes, so tests can observe the in-flight state. */
    var refreshGate: CompletableDeferred<Unit>? = null

    override fun observeMakes(): Flow<List<Make>> = makes

    override fun observeMake(makeId: Int): Flow<Make?> = makes.map { list -> list.firstOrNull { it.id == makeId } }

    override fun observeModels(makeId: Int): Flow<List<Model>> = flowOf(emptyList())

    override suspend fun refreshMakes(force: Boolean): Result<Unit, DataError> {
        refreshForceCalls += force
        refreshGate?.await()
        if (refreshResult is Result.Success) makesToPublishOnRefresh?.let { makes.value = it }
        return refreshResult
    }

    override suspend fun refreshModels(
        makeId: Int,
        force: Boolean,
    ): Result<Unit, DataError> = refreshResult
}

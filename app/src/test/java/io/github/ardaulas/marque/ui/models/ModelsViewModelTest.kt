package io.github.ardaulas.marque.ui.models

import app.cash.turbine.test
import io.github.ardaulas.marque.core.result.DataError
import io.github.ardaulas.marque.core.result.Result
import io.github.ardaulas.marque.domain.model.Make
import io.github.ardaulas.marque.domain.model.Model
import io.github.ardaulas.marque.ui.FakeVehicleRepository
import io.github.ardaulas.marque.ui.navigation.ModelsRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ModelsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeVehicleRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository.makes.value = listOf(Make(TOYOTA_ID, "TOYOTA"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // In production RouteModule decodes ModelsRoute from the SavedStateHandle with toRoute(),
    // which needs an Android Bundle; here the route is passed directly so the test stays on the JVM.
    private fun viewModel(makeId: Int = TOYOTA_ID) = ModelsViewModel(repository, ModelsRoute(makeId))

    @Test
    fun `starts in Loading with the make name and shows Content once the refresh succeeds`() =
        runTest(dispatcher) {
            repository.refreshResult = Result.Success(Unit)
            repository.modelsToPublishOnRefresh = listOf(Model(2208, "Corolla"))

            val viewModel = viewModel()

            viewModel.uiState.test {
                assertEquals(ModelsUiState.Loading(makeName = null), awaitItem())
                advanceUntilIdle()
                assertEquals(
                    ModelsUiState.Content("TOYOTA", listOf(Model(2208, "Corolla")), isRefreshing = false, error = null),
                    expectMostRecentItem(),
                )
            }
        }

    @Test
    fun `no cache plus a failing refresh is Error, still titled with the make`() =
        runTest(dispatcher) {
            repository.refreshResult = Result.Failure(DataError.Network)

            val viewModel = viewModel()

            viewModel.uiState.test {
                advanceUntilIdle()
                assertEquals(ModelsUiState.Error("TOYOTA", DataError.Network), expectMostRecentItem())
            }
        }

    @Test
    fun `cached models plus a failing refresh is Content with the error, not Error`() =
        runTest(dispatcher) {
            repository.models.value = mapOf(TOYOTA_ID to listOf(Model(2208, "Corolla")))
            repository.refreshResult = Result.Failure(DataError.Http(503))

            val viewModel = viewModel()

            viewModel.uiState.test {
                advanceUntilIdle()
                assertEquals(
                    ModelsUiState.Content("TOYOTA", listOf(Model(2208, "Corolla")), isRefreshing = false, error = DataError.Http(503)),
                    expectMostRecentItem(),
                )
            }
        }

    @Test
    fun `no cache and a successful but empty refresh is Empty`() =
        runTest(dispatcher) {
            repository.refreshResult = Result.Success(Unit)

            val viewModel = viewModel()

            viewModel.uiState.test {
                advanceUntilIdle()
                assertEquals(ModelsUiState.Empty("TOYOTA"), expectMostRecentItem())
            }
        }

    @Test
    fun `the initial refresh targets the route's make and is not forced, a user retry is forced`() =
        runTest(dispatcher) {
            repository.refreshResult = Result.Failure(DataError.Timeout)
            val viewModel = viewModel()

            viewModel.uiState.test {
                advanceUntilIdle()
                assertEquals(listOf(TOYOTA_ID to false), repository.refreshModelsCalls)

                repository.refreshResult = Result.Success(Unit)
                repository.modelsToPublishOnRefresh = listOf(Model(2206, "Scion xA"))
                viewModel.refresh()
                advanceUntilIdle()

                assertEquals(listOf(TOYOTA_ID to false, TOYOTA_ID to true), repository.refreshModelsCalls)
                assertEquals(
                    ModelsUiState.Content("TOYOTA", listOf(Model(2206, "Scion xA")), isRefreshing = false, error = null),
                    expectMostRecentItem(),
                )
            }
        }

    @Test
    fun `only the route's make's models are observed`() =
        runTest(dispatcher) {
            repository.models.value = mapOf(TOYOTA_ID to listOf(Model(2208, "Corolla")), 474 to listOf(Model(1861, "Civic")))
            repository.refreshResult = Result.Success(Unit)

            val viewModel = viewModel()

            viewModel.uiState.test {
                advanceUntilIdle()
                val content = expectMostRecentItem() as ModelsUiState.Content
                assertEquals(listOf(Model(2208, "Corolla")), content.models)
            }
        }

    private companion object {
        const val TOYOTA_ID = 448
    }
}

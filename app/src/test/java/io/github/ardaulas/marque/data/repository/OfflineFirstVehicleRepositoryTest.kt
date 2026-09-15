package io.github.ardaulas.marque.data.repository

import app.cash.turbine.test
import io.github.ardaulas.marque.core.result.DataError
import io.github.ardaulas.marque.core.result.Result
import io.github.ardaulas.marque.data.local.VehicleLocalDataSource
import io.github.ardaulas.marque.data.local.entity.MakeEntity
import io.github.ardaulas.marque.data.local.entity.ModelEntity
import io.github.ardaulas.marque.data.local.entity.RefreshKeys
import io.github.ardaulas.marque.data.remote.VehicleRemoteDataSource
import io.github.ardaulas.marque.data.remote.dto.MakeDto
import io.github.ardaulas.marque.data.remote.dto.ModelDto
import io.github.ardaulas.marque.domain.model.Make
import io.github.ardaulas.marque.domain.model.Model
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class OfflineFirstVehicleRepositoryTest {
    private val clock = MutableClock(Instant.parse("2026-09-15T12:00:00Z"))
    private val remote = FakeVehicleRemoteDataSource()
    private val local = FakeVehicleLocalDataSource()
    private val repository = OfflineFirstVehicleRepository(remote, local, clock)

    @Test
    fun `refreshMakes success writes the cache and observers see the new list`() =
        runTest {
            remote.makesResult = Result.Success(listOf(MakeDto(441, "TESLA"), MakeDto(440, "ASTON MARTIN")))

            repository.observeMakes().test {
                assertEquals(emptyList<Make>(), awaitItem())

                assertEquals(Result.Success(Unit), repository.refreshMakes())

                assertEquals(listOf(Make(440, "ASTON MARTIN"), Make(441, "TESLA")), awaitItem())
            }
            assertEquals(clock.millis(), local.lastRefreshedAt(RefreshKeys.MAKES))
        }

    @Test
    fun `refreshMakes de-duplicates a repeated MakeId`() =
        runTest {
            remote.makesResult = Result.Success(listOf(MakeDto(441, "TESLA"), MakeDto(441, "TESLA")))

            repository.refreshMakes()

            assertEquals(listOf(Make(441, "TESLA")), repository.observeMakes().first())
        }

    @Test
    fun `refreshMakes failure returns the mapped error and keeps the old cache`() =
        runTest {
            local.seedMakes(listOf(MakeEntity(440, "ASTON MARTIN")), refreshedAt = clock.millis() - Duration.ofDays(2).toMillis())
            remote.makesResult = Result.Failure(DataError.Network)

            assertEquals(Result.Failure(DataError.Network), repository.refreshMakes())

            assertEquals(listOf(Make(440, "ASTON MARTIN")), repository.observeMakes().first())
            assertEquals(1, remote.makesCalls)
        }

    @Test
    fun `a fresh cache skips the network unless forced`() =
        runTest {
            local.seedMakes(listOf(MakeEntity(440, "ASTON MARTIN")), refreshedAt = clock.millis())
            remote.makesResult = Result.Success(listOf(MakeDto(441, "TESLA")))
            clock.advance(Duration.ofHours(23))

            assertEquals(Result.Success(Unit), repository.refreshMakes())
            assertEquals(0, remote.makesCalls)
            assertEquals(listOf(Make(440, "ASTON MARTIN")), repository.observeMakes().first())

            assertEquals(Result.Success(Unit), repository.refreshMakes(force = true))
            assertEquals(1, remote.makesCalls)
            assertEquals(listOf(Make(441, "TESLA")), repository.observeMakes().first())
        }

    @Test
    fun `a cache older than 24 hours is refreshed`() =
        runTest {
            local.seedMakes(listOf(MakeEntity(440, "ASTON MARTIN")), refreshedAt = clock.millis())
            remote.makesResult = Result.Success(listOf(MakeDto(441, "TESLA")))
            clock.advance(Duration.ofHours(24))

            assertEquals(Result.Success(Unit), repository.refreshMakes())

            assertEquals(1, remote.makesCalls)
            assertEquals(listOf(Make(441, "TESLA")), repository.observeMakes().first())
        }

    @Test
    fun `refreshModels writes only that make's models and stamps that make's key`() =
        runTest {
            local.seedMakes(listOf(MakeEntity(448, "Toyota"), MakeEntity(449, "Honda")), refreshedAt = clock.millis())
            local.seedModels(449, listOf(ModelEntity(1, 449, "Civic")))
            remote.modelsResult = Result.Success(listOf(ModelDto(makeId = 448, id = 2206, name = "Scion xA")))

            assertEquals(Result.Success(Unit), repository.refreshModels(448))

            assertEquals(listOf(Model(2206, "Scion xA")), repository.observeModels(448).first())
            assertEquals(listOf(Model(1, "Civic")), repository.observeModels(449).first())
            assertEquals(clock.millis(), local.lastRefreshedAt(RefreshKeys.models(448)))
            assertNull(local.lastRefreshedAt(RefreshKeys.models(449)))
            assertEquals(448, remote.lastModelsMakeId)
        }

    @Test
    fun `refreshModels failure keeps that make's cached models`() =
        runTest {
            local.seedModels(448, listOf(ModelEntity(2206, 448, "Scion xA")))
            remote.modelsResult = Result.Failure(DataError.Http(500))

            assertEquals(Result.Failure(DataError.Http(500)), repository.refreshModels(448))

            assertEquals(listOf(Model(2206, "Scion xA")), repository.observeModels(448).first())
        }

    @Test
    fun `a failing cache write surfaces as Unknown instead of throwing`() =
        runTest {
            remote.makesResult = Result.Success(listOf(MakeDto(441, "TESLA")))
            local.failNextWrite = true

            assertEquals(Result.Failure(DataError.Unknown), repository.refreshMakes())
        }

    @Test
    fun `observeMake emits the cached make or null`() =
        runTest {
            local.seedMakes(listOf(MakeEntity(440, "ASTON MARTIN")), refreshedAt = clock.millis())

            assertEquals(Make(440, "ASTON MARTIN"), repository.observeMake(440).first())
            assertNull(repository.observeMake(999).first())
        }
}

/** A [Clock] whose time only moves when the test says so. */
private class MutableClock(
    private var now: Instant,
    private val zone: ZoneId = ZoneOffset.UTC,
) : Clock() {
    fun advance(by: Duration) {
        now = now.plus(by)
    }

    override fun instant(): Instant = now

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = MutableClock(now, zone)
}

private class FakeVehicleRemoteDataSource : VehicleRemoteDataSource {
    var makesResult: Result<List<MakeDto>, DataError> = Result.Success(emptyList())
    var modelsResult: Result<List<ModelDto>, DataError> = Result.Success(emptyList())
    var makesCalls = 0
        private set
    var lastModelsMakeId: Int? = null
        private set

    override suspend fun fetchMakes(): Result<List<MakeDto>, DataError> {
        makesCalls++
        return makesResult
    }

    override suspend fun fetchModels(makeId: Int): Result<List<ModelDto>, DataError> {
        lastModelsMakeId = makeId
        return modelsResult
    }
}

/** In-memory stand-in for the Room-backed source; sorted the same way the DAO queries sort. */
private class FakeVehicleLocalDataSource : VehicleLocalDataSource {
    private val makes = MutableStateFlow<Map<Int, MakeEntity>>(emptyMap())
    private val models = MutableStateFlow<Map<Int, ModelEntity>>(emptyMap())
    private val refreshedAt = mutableMapOf<String, Long>()
    var failNextWrite = false

    fun seedMakes(
        entities: List<MakeEntity>,
        refreshedAt: Long,
    ) {
        makes.value = entities.associateBy { it.id }
        this.refreshedAt[RefreshKeys.MAKES] = refreshedAt
    }

    fun seedModels(
        makeId: Int,
        entities: List<ModelEntity>,
    ) {
        models.value = models.value + entities.associateBy { it.id }
        check(entities.all { it.makeId == makeId })
    }

    override fun observeMakes(): Flow<List<MakeEntity>> = makes.map { it.values.sortedBy { make -> make.name.lowercase() } }

    override fun observeMake(makeId: Int): Flow<MakeEntity?> = makes.map { it[makeId] }

    override fun observeModels(makeId: Int): Flow<List<ModelEntity>> =
        models.map { all -> all.values.filter { it.makeId == makeId }.sortedBy { it.name.lowercase() } }

    override suspend fun lastRefreshedAt(key: String): Long? = refreshedAt[key]

    override suspend fun replaceMakes(
        makes: List<MakeEntity>,
        refreshedAtEpochMillis: Long,
    ) {
        maybeFail()
        this.makes.value = makes.associateBy { it.id }
        refreshedAt[RefreshKeys.MAKES] = refreshedAtEpochMillis
    }

    override suspend fun replaceModels(
        makeId: Int,
        models: List<ModelEntity>,
        refreshedAtEpochMillis: Long,
    ) {
        maybeFail()
        this.models.value = this.models.value.filterValues { it.makeId != makeId } + models.associateBy { it.id }
        refreshedAt[RefreshKeys.models(makeId)] = refreshedAtEpochMillis
    }

    private fun maybeFail() {
        if (failNextWrite) {
            failNextWrite = false
            throw IllegalStateException("simulated constraint failure")
        }
    }
}

package io.github.ardaulas.marque.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import io.github.ardaulas.marque.data.local.dao.MakeDao
import io.github.ardaulas.marque.data.local.dao.ModelDao
import io.github.ardaulas.marque.data.local.entity.MakeEntity
import io.github.ardaulas.marque.data.local.entity.ModelEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs the real generated DAOs against an in-memory SQLite database on the device. */
@RunWith(AndroidJUnit4::class)
class VehicleDaoTest {
    private lateinit var database: MarqueDatabase
    private lateinit var makeDao: MakeDao
    private lateinit var modelDao: ModelDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, MarqueDatabase::class.java).build()
        makeDao = database.makeDao()
        modelDao = database.modelDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun modelsFlowEmitsAfterTheReplaceTransaction() =
        runTest {
            makeDao.replaceAll(listOf(MakeEntity(448, "Toyota")))

            modelDao.observeByMake(448).test {
                assertEquals(emptyList<ModelEntity>(), awaitItem())

                modelDao.replaceForMake(448, listOf(ModelEntity(2207, 448, "Scion tC"), ModelEntity(2206, 448, "Scion xA")))

                assertEquals(listOf(ModelEntity(2207, 448, "Scion tC"), ModelEntity(2206, 448, "Scion xA")), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun replacingOneMakesModelsLeavesAnotherMakesModelsAlone() =
        runTest {
            makeDao.replaceAll(listOf(MakeEntity(448, "Toyota"), MakeEntity(474, "Honda")))
            modelDao.replaceForMake(448, listOf(ModelEntity(2206, 448, "Scion xA")))
            modelDao.replaceForMake(474, listOf(ModelEntity(1861, 474, "Civic")))

            modelDao.replaceForMake(448, listOf(ModelEntity(2208, 448, "Corolla")))

            assertEquals(listOf(ModelEntity(2208, 448, "Corolla")), modelDao.observeByMake(448).first())
            assertEquals(listOf(ModelEntity(1861, 474, "Civic")), modelDao.observeByMake(474).first())
        }

    @Test
    fun replaceAllUpsertsNewRowsDeletesMissingOnesAndCascadesTheirModels() =
        runTest {
            makeDao.replaceAll(listOf(MakeEntity(448, "Toyota"), MakeEntity(474, "Honda")))
            modelDao.replaceForMake(474, listOf(ModelEntity(1861, 474, "Civic")))

            makeDao.replaceAll(listOf(MakeEntity(448, "TOYOTA"), MakeEntity(441, "TESLA")))

            assertEquals(listOf(MakeEntity(441, "TESLA"), MakeEntity(448, "TOYOTA")), makeDao.observeAll().first())
            assertEquals(emptyList<ModelEntity>(), modelDao.observeByMake(474).first())
        }

    @Test
    fun replaceAllWithAnEmptyListClearsTheTable() =
        runTest {
            makeDao.replaceAll(listOf(MakeEntity(448, "Toyota")))

            makeDao.replaceAll(emptyList())

            assertEquals(emptyList<MakeEntity>(), makeDao.observeAll().first())
        }
}

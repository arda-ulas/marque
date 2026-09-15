package io.github.ardaulas.marque.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.ardaulas.marque.data.local.MarqueDatabase
import io.github.ardaulas.marque.data.local.RoomVehicleLocalDataSource
import io.github.ardaulas.marque.data.local.VehicleLocalDataSource
import io.github.ardaulas.marque.data.local.dao.MakeDao
import io.github.ardaulas.marque.data.local.dao.ModelDao
import io.github.ardaulas.marque.data.local.dao.RefreshMetadataDao
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): MarqueDatabase = Room.databaseBuilder(context, MarqueDatabase::class.java, MarqueDatabase.NAME).build()

    @Provides
    fun provideMakeDao(database: MarqueDatabase): MakeDao = database.makeDao()

    @Provides
    fun provideModelDao(database: MarqueDatabase): ModelDao = database.modelDao()

    @Provides
    fun provideRefreshMetadataDao(database: MarqueDatabase): RefreshMetadataDao = database.refreshMetadataDao()

    /** Wall-clock time for the cache policy; tests substitute a controllable clock. */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class LocalDataSourceModule {
    @Binds
    @Singleton
    abstract fun bindVehicleLocalDataSource(impl: RoomVehicleLocalDataSource): VehicleLocalDataSource
}

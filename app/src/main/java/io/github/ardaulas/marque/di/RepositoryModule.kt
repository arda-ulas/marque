package io.github.ardaulas.marque.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.ardaulas.marque.data.repository.OfflineFirstVehicleRepository
import io.github.ardaulas.marque.data.repository.VehicleRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindVehicleRepository(impl: OfflineFirstVehicleRepository): VehicleRepository
}

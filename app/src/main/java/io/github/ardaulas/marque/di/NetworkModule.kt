package io.github.ardaulas.marque.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.ardaulas.marque.BuildConfig
import io.github.ardaulas.marque.data.remote.RetryPolicy
import io.github.ardaulas.marque.data.remote.VehicleRemoteDataSource
import io.github.ardaulas.marque.data.remote.VpicApi
import io.github.ardaulas.marque.data.remote.VpicRemoteDataSource
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    /** Shared with the MockWebServer test so the parser under test is the production one. */
    val json: Json = Json { ignoreUnknownKeys = true }

    /** Builds the production Retrofit stack against any base URL; tests point it at MockWebServer. */
    fun createRetrofit(
        client: OkHttpClient,
        baseUrl: String,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                }
            }.build()

    @Provides
    @Singleton
    fun provideVpicApi(client: OkHttpClient): VpicApi = createRetrofit(client, VpicApi.BASE_URL).create(VpicApi::class.java)

    @Provides
    @Singleton
    fun provideRetryPolicy(): RetryPolicy = RetryPolicy()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteDataSourceModule {
    @Binds
    @Singleton
    abstract fun bindVehicleRemoteDataSource(impl: VpicRemoteDataSource): VehicleRemoteDataSource
}

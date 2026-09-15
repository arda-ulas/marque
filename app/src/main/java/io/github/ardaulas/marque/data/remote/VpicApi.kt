package io.github.ardaulas.marque.data.remote

import io.github.ardaulas.marque.data.remote.dto.MakeDto
import io.github.ardaulas.marque.data.remote.dto.ModelDto
import io.github.ardaulas.marque.data.remote.dto.VpicResponse
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * NHTSA vPIC endpoints used by the app. No authentication; `format=json` is part of the
 * relative URL because the API defaults to XML.
 */
interface VpicApi {
    @GET("vehicles/GetMakesForVehicleType/car?format=json")
    suspend fun getMakesForCars(): VpicResponse<MakeDto>

    @GET("vehicles/GetModelsForMakeId/{makeId}?format=json")
    suspend fun getModelsForMake(
        @Path("makeId") makeId: Int,
    ): VpicResponse<ModelDto>

    companion object {
        const val BASE_URL = "https://vpic.nhtsa.dot.gov/api/"
    }
}

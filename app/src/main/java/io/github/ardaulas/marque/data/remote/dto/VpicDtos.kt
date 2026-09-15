package io.github.ardaulas.marque.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * vPIC wraps every list endpoint in the same envelope. Only the fields the app reads are
 * declared; `Count`, `Message`, and `SearchCriteria` are dropped by `ignoreUnknownKeys`.
 */
@Serializable
data class VpicResponse<T>(
    @SerialName("Results") val results: List<T>,
)

/** One row of `GetMakesForVehicleType/car`. The API returns make names in upper case. */
@Serializable
data class MakeDto(
    @SerialName("MakeId") val id: Int,
    @SerialName("MakeName") val name: String,
)

/** One row of `GetModelsForMakeId/{makeId}`. Note the different key style from [MakeDto]. */
@Serializable
data class ModelDto(
    @SerialName("Make_ID") val makeId: Int,
    @SerialName("Model_ID") val id: Int,
    @SerialName("Model_Name") val name: String,
)

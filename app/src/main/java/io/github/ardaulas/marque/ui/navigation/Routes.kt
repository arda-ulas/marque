package io.github.ardaulas.marque.ui.navigation

import kotlinx.serialization.Serializable

/** Start destination: the list of makes. */
@Serializable
data object MakesRoute

/** One make's models. [makeId] is the vPIC make id and travels as a typed navigation argument. */
@Serializable
data class ModelsRoute(
    val makeId: Int,
)

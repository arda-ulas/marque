package io.github.ardaulas.marque.domain.model

/** A car make as shown in the UI. Names are kept exactly as vPIC returns them. */
data class Make(
    val id: Int,
    val name: String,
)

/** A model belonging to one [Make]. */
data class Model(
    val id: Int,
    val name: String,
)

package io.github.ardaulas.marque.data

import javax.inject.Inject

/**
 * Placeholder repository used to prove the DI wiring end to end
 * (Hilt -> ViewModel -> Compose). It will be replaced by the real data layer.
 */
class HelloRepository
    @Inject
    constructor() {
        fun greeting(): String = "Hello from Marque"
    }

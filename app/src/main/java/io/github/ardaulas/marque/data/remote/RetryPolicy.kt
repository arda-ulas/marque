package io.github.ardaulas.marque.data.remote

import io.github.ardaulas.marque.core.result.DataError
import io.github.ardaulas.marque.core.result.Result
import io.github.ardaulas.marque.core.result.isRetryable
import kotlinx.coroutines.delay

/**
 * Retries a call exactly once, after [delayMillis], when the first attempt fails with a
 * transient error (see [isRetryable]). Deterministic failures return immediately.
 *
 * The delay goes through [delay] so tests can run it on virtual time.
 */
class RetryPolicy(
    val delayMillis: Long = DEFAULT_DELAY_MILLIS,
) {
    suspend fun <D> retryOnce(block: suspend () -> Result<D, DataError>): Result<D, DataError> {
        val first = block()
        if (first !is Result.Failure || !first.error.isRetryable) return first
        delay(delayMillis)
        return block()
    }

    companion object {
        const val DEFAULT_DELAY_MILLIS = 1_000L
    }
}

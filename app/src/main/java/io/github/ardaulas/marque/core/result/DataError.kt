package io.github.ardaulas.marque.core.result

/**
 * Every way a data operation can fail, as seen by the UI. Mapping from exceptions happens once,
 * in the remote data source, so the rest of the app only ever deals with these cases.
 */
sealed interface DataError {
    /** No connectivity, DNS failure, connection reset: any I/O failure that is not a timeout. */
    data object Network : DataError

    /** Connect, read, or whole-call timeout. */
    data object Timeout : DataError

    /** The server answered with a non-2xx status. */
    data class Http(
        val code: Int,
    ) : DataError

    /** The body could not be decoded into the expected DTO. */
    data object Serialization : DataError

    /** Anything else; treated as a bug to investigate rather than a condition to retry. */
    data object Unknown : DataError
}

/**
 * Whether a second attempt has a realistic chance of succeeding. Client errors (4xx) and
 * decoding failures are deterministic, so retrying them only adds latency.
 */
val DataError.isRetryable: Boolean
    get() =
        when (this) {
            DataError.Network, DataError.Timeout -> true
            is DataError.Http -> code in 500..599
            DataError.Serialization, DataError.Unknown -> false
        }

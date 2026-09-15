package io.github.ardaulas.marque.data.remote

import io.github.ardaulas.marque.core.result.DataError
import io.github.ardaulas.marque.core.result.Result
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException

/**
 * Runs one network call and converts any failure into a [DataError]. This is the only place
 * where transport exceptions are caught; everything above it works with [Result].
 *
 * [CancellationException] is always rethrown so structured concurrency keeps working.
 */
suspend inline fun <D> safeCall(crossinline block: suspend () -> D): Result<D, DataError> =
    try {
        Result.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.Failure(e.toDataError())
    }

/**
 * Order matters: [SocketTimeoutException] and OkHttp's call-timeout [InterruptedIOException]
 * are both [IOException]s, so they are matched before the generic I/O case.
 */
fun Throwable.toDataError(): DataError =
    when (this) {
        is HttpException -> DataError.Http(code())
        is SocketTimeoutException, is InterruptedIOException -> DataError.Timeout
        is IOException -> DataError.Network
        is SerializationException -> DataError.Serialization
        else -> DataError.Unknown
    }

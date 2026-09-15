package io.github.ardaulas.marque.core.result

/**
 * Outcome of an operation that can fail with a typed error.
 *
 * Deliberately minimal: two cases and one mapper. Exceptions are converted into an [Failure]
 * at the data-layer boundary so callers above the repository never have to catch anything.
 */
sealed interface Result<out D, out E> {
    data class Success<out D>(
        val data: D,
    ) : Result<D, Nothing>

    data class Failure<out E>(
        val error: E,
    ) : Result<Nothing, E>
}

inline fun <D, E, R> Result<D, E>.map(transform: (D) -> R): Result<R, E> =
    when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Failure -> this
    }

fun <E> Result<*, E>.errorOrNull(): E? = (this as? Result.Failure)?.error

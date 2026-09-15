package io.github.ardaulas.marque.data.remote

import io.github.ardaulas.marque.core.result.DataError
import io.github.ardaulas.marque.core.result.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RetryPolicyTest {
    private val policy = RetryPolicy(delayMillis = 1_000)

    /** Returns the scripted results in order and counts how many times it was invoked. */
    private class ScriptedCall(
        vararg results: Result<String, DataError>,
    ) {
        private val queue = ArrayDeque(results.toList())
        var attempts = 0
            private set

        suspend fun invoke(): Result<String, DataError> {
            attempts++
            return queue.removeFirst()
        }
    }

    @Test
    fun `a network failure is retried once after the delay and the retry result wins`() =
        runTest {
            val call = ScriptedCall(Result.Failure(DataError.Network), Result.Success("ok"))

            val result = policy.retryOnce { call.invoke() }

            assertEquals(Result.Success("ok"), result)
            assertEquals(2, call.attempts)
            assertEquals(1_000, currentTime)
        }

    @Test
    fun `a timeout is retried once`() =
        runTest {
            val call = ScriptedCall(Result.Failure(DataError.Timeout), Result.Success("ok"))

            assertEquals(Result.Success("ok"), policy.retryOnce { call.invoke() })
            assertEquals(2, call.attempts)
        }

    @Test
    fun `a 5xx is retried once and the second failure is returned`() =
        runTest {
            val call = ScriptedCall(Result.Failure(DataError.Http(502)), Result.Failure(DataError.Http(500)))

            assertEquals(Result.Failure(DataError.Http(500)), policy.retryOnce { call.invoke() })
            assertEquals(2, call.attempts)
            assertEquals(1_000, currentTime)
        }

    @Test
    fun `a 4xx is not retried`() =
        runTest {
            val call = ScriptedCall(Result.Failure(DataError.Http(404)))

            assertEquals(Result.Failure(DataError.Http(404)), policy.retryOnce { call.invoke() })
            assertEquals(1, call.attempts)
            assertEquals(0, currentTime)
        }

    @Test
    fun `a serialization failure is not retried`() =
        runTest {
            val call = ScriptedCall(Result.Failure(DataError.Serialization))

            assertEquals(Result.Failure(DataError.Serialization), policy.retryOnce { call.invoke() })
            assertEquals(1, call.attempts)
        }

    @Test
    fun `success on the first attempt is returned without waiting`() =
        runTest {
            val call = ScriptedCall(Result.Success("ok"))

            assertEquals(Result.Success("ok"), policy.retryOnce { call.invoke() })
            assertEquals(1, call.attempts)
            assertEquals(0, currentTime)
        }
}

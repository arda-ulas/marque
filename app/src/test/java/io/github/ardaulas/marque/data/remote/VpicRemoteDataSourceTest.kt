package io.github.ardaulas.marque.data.remote

import io.github.ardaulas.marque.core.result.DataError
import io.github.ardaulas.marque.core.result.Result
import io.github.ardaulas.marque.data.remote.dto.MakeDto
import io.github.ardaulas.marque.data.remote.dto.ModelDto
import io.github.ardaulas.marque.di.NetworkModule
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Exercises the real Retrofit + kotlinx.serialization + OkHttp stack against MockWebServer.
 * Retry delay is zero here; the virtual-time behaviour of the delay is covered by
 * [RetryPolicyTest].
 */
class VpicRemoteDataSourceTest {
    private val server = MockWebServer()
    private lateinit var dataSource: VpicRemoteDataSource

    @Before
    fun setUp() {
        server.start()
        val client =
            OkHttpClient
                .Builder()
                .connectTimeout(1, TimeUnit.SECONDS)
                .readTimeout(250, TimeUnit.MILLISECONDS)
                .callTimeout(2, TimeUnit.SECONDS)
                // OkHttp's own transparent retry would hide the count of attempts under test.
                .retryOnConnectionFailure(false)
                .build()
        val api = NetworkModule.createRetrofit(client, server.url("/").toString()).create(VpicApi::class.java)
        dataSource = VpicRemoteDataSource(api, RetryPolicy(delayMillis = 0))
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `200 with the vPIC envelope parses into DTOs and ignores unknown keys`() =
        runTest {
            server.enqueue(MockResponse(body = MAKES_BODY))

            val result = dataSource.fetchMakes()

            assertEquals(
                Result.Success(listOf(MakeDto(id = 440, name = "ASTON MARTIN"), MakeDto(id = 441, name = "TESLA"))),
                result,
            )
            assertEquals(1, server.requestCount)
            assertEquals("/vehicles/GetMakesForVehicleType/car?format=json", server.takeRequest().target)
        }

    @Test
    fun `models endpoint maps the underscore keys and interpolates the make id`() =
        runTest {
            server.enqueue(MockResponse(body = MODELS_BODY))

            val result = dataSource.fetchModels(448)

            assertEquals(Result.Success(listOf(ModelDto(makeId = 448, id = 2206, name = "Scion xA"))), result)
            assertEquals("/vehicles/GetModelsForMakeId/448?format=json", server.takeRequest().target)
        }

    @Test
    fun `500 is retried once and then reported as Http 500`() =
        runTest {
            server.enqueue(MockResponse(code = 500))
            server.enqueue(MockResponse(code = 500))

            val result = dataSource.fetchMakes()

            assertEquals(Result.Failure(DataError.Http(500)), result)
            assertEquals(2, server.requestCount)
        }

    @Test
    fun `500 then 200 recovers on the retry`() =
        runTest {
            server.enqueue(MockResponse(code = 503))
            server.enqueue(MockResponse(body = MAKES_BODY))

            val result = dataSource.fetchMakes()

            assertEquals(2, (result as Result.Success).data.size)
            assertEquals(2, server.requestCount)
        }

    @Test
    fun `404 is not retried and is reported as Http 404`() =
        runTest {
            server.enqueue(MockResponse(code = 404))

            val result = dataSource.fetchMakes()

            assertEquals(Result.Failure(DataError.Http(404)), result)
            assertEquals(1, server.requestCount)
        }

    @Test
    fun `malformed JSON is not retried and is reported as Serialization`() =
        runTest {
            server.enqueue(MockResponse(body = """{"Results": [{"MakeId": "not-a-number"}]"""))

            val result = dataSource.fetchMakes()

            assertEquals(Result.Failure(DataError.Serialization), result)
            assertEquals(1, server.requestCount)
        }

    @Test
    fun `a stalled response hits the read timeout, is retried once, and is reported as Timeout`() =
        runTest {
            val stall = MockResponse.Builder().onResponseStart(SocketEffect.Stall).build()
            server.enqueue(stall)
            server.enqueue(stall)

            val result = dataSource.fetchMakes()

            assertEquals(Result.Failure(DataError.Timeout), result)
            assertEquals(2, server.requestCount)
        }

    @Test
    fun `a dropped connection is reported as Network`() =
        runTest {
            val disconnect = MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build()
            server.enqueue(disconnect)
            server.enqueue(disconnect)

            val result = dataSource.fetchMakes()

            assertEquals(Result.Failure(DataError.Network), result)
            assertEquals(2, server.requestCount)
        }

    private companion object {
        val MAKES_BODY =
            """
            {"Count":2,"Message":"Response returned successfully","SearchCriteria":"Vehicle Type: car",
             "Results":[
               {"MakeId":440,"MakeName":"ASTON MARTIN","VehicleTypeId":2,"VehicleTypeName":"Passenger Car"},
               {"MakeId":441,"MakeName":"TESLA","VehicleTypeId":2,"VehicleTypeName":"Passenger Car"}
             ]}
            """.trimIndent()

        val MODELS_BODY =
            """
            {"Count":1,"Message":"Response returned successfully","SearchCriteria":"MakeId:448",
             "Results":[{"Make_ID":448,"Make_Name":"Toyota","Model_ID":2206,"Model_Name":"Scion xA"}]}
            """.trimIndent()
    }
}

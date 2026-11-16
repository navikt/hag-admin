package no.nav.hag

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authentication
import io.ktor.server.auth.basic
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import no.nav.hag.plugins.configureRouting
import no.nav.helsearbeidsgiver.utils.json.toJson
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.milliseconds

class ForespoerselRoutingTest {
    @Test
    fun `hentEksponerteForespoersler returnerer forespoerslene som JSON`() =
        testApplication {
            val service = mockk<ForespoerselService>()
            val fnr = "12345678901"
            val forespoersler =
                listOf(
                    JsonObject(
                        mapOf(
                            "forespoerselId" to UUID.randomUUID().toJson(),
                            "status" to "Aktiv".toJson(),
                            "fnr" to fnr.toJson(),
                        ),
                    ),
                    JsonObject(
                        mapOf(
                            "forespoerselId" to UUID.randomUUID().toJson(),
                            "status" to "Forkastet".toJson(),
                            "fnr" to fnr.toJson(),
                        ),
                    ),
                )
            coEvery { service.hentEksponerteForespoersler(fnr) } returns forespoersler
            application { configureTestRouting(service) }

            val response =
                client.post("/hentEksponerteForespoersler") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"fnr":"$fnr"}""")
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType.Application.Json, response.contentType()?.withoutParameters())
            assertEquals(JsonArray(forespoersler), Json.parseToJsonElement(response.bodyAsText()))
            coVerify(exactly = 1) { service.hentEksponerteForespoersler(fnr) }
        }

    @Test
    fun `forkastForespoersel venter paa bekreftelse med riktig ID`() =
        testApplication {
            val service = mockk<ForespoerselService>()
            val forespoerselId = UUID.randomUUID()
            val forkastingStartet = CompletableDeferred<Unit>()
            val bekreftelse = CompletableDeferred<Unit>()
            coEvery { service.forkastForespoerselOgVentPaaBekreftelse(forespoerselId, any()) } coAnswers {
                forkastingStartet.complete(Unit)
                bekreftelse.await()
            }
            application { configureTestRouting(service) }

            withTimeout(5_000.milliseconds) {
                val response =
                    async {
                        client.post("/forkastForespoersel") {
                            contentType(ContentType.Application.Json)
                            setBody("""{"forespoerselId":"$forespoerselId"}""")
                        }
                    }
                forkastingStartet.await()
                assertFalse(response.isCompleted)
                bekreftelse.complete(Unit)
                assertEquals(HttpStatusCode.OK, response.await().status)
            }

            coVerify(exactly = 1) { service.forkastForespoerselOgVentPaaBekreftelse(forespoerselId, any()) }
            coVerify(exactly = 0) { service.forkastForespoersel(any(), any()) }
        }

    private fun Application.configureTestRouting(service: ForespoerselService) {
        install(ContentNegotiation) { json() }
        authentication {
            basic {
                skipWhen { true }
            }
        }
        configureRouting(
            notifikasjonService = mockk(relaxed = true),
            forespoerselService = service,
            appMicrometerRegistry = mockk(relaxed = true),
            brregClient = mockk(relaxed = true),
        )
    }
}

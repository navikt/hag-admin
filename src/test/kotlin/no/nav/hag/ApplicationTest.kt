package no.nav.hag

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.hag.plugins.GROUP_ID_HAG
import no.nav.hag.plugins.configureRouting
import no.nav.hag.plugins.configureSecurity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {
    @Test
    fun testRoot() =
        testApplication {
            val authClient = mockk<AuthClient>()
            val appMicrometerRegistry = mockk<PrometheusMeterRegistry>()

            val mockToken =
                JWT
                    .create()
                    .withClaim("NAVident", "testbruker")
                    .withArrayClaim("groups", arrayOf(GROUP_ID_HAG))
                    .sign(Algorithm.HMAC256("test-secret"))

            coEvery { authClient.introspect(mockToken) } returns true
            application {
                configureSecurity(authClient, disabled = false)
                configureRouting(MockForespoerselService(), appMicrometerRegistry)
            }

            val response =
                client.get("/") {
                    bearerAuth(mockToken)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType.Text.Html, response.contentType()?.withoutParameters())
            assertTrue(response.bodyAsText().contains("""<div id="root"></div>"""))
            coVerify(exactly = 1) { authClient.introspect(mockToken) }
        }

    @Test
    fun testRootUtenToken() =
        testApplication {
            val authClient = mockk<AuthClient>()
            application {
                configureSecurity(authClient, disabled = false)
                configureRouting(MockForespoerselService(), mockk<PrometheusMeterRegistry>())
            }

            assertEquals(HttpStatusCode.Unauthorized, client.get("/").status)
            coVerify(exactly = 0) { authClient.introspect(any()) }
        }
}

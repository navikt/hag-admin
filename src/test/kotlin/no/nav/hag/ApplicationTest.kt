package no.nav.hag

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.ktor.test.dispatcher.testSuspend
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.hag.plugins.GROUP_ID_HAG
import no.nav.hag.plugins.configureRouting
import no.nav.hag.plugins.configureSecurity
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testRoot() =
        testApplication {
            val authClient = mockk<AuthClient>()

            val mockToken =
                JWT
                    .create()
                    .withClaim("NAVident", "John Ronald Reuel")
                    .withArrayClaim("groups", arrayOf(GROUP_ID_HAG))
                    .sign(Algorithm.HMAC256("super secret"))

            coEvery { authClient.introspect(mockToken) } returns true

            application {
                configureSecurity(authClient, disabled = false)
                configureRouting(FakeServiceImpl())
            }

            client
                .get("/") {
                    headers {
                        append("Authorization", "Bearer $mockToken")
                    }
                }.apply {
                    println(bodyAsText())
                    assertEquals(HttpStatusCode.OK, status)
                }
            client.get("/admin-ui/ferdigstillOppgaver-form.html").apply {
                assertEquals(HttpStatusCode.OK, status)
            }
        }

    @Test
    fun testKlient() {
        val url = "https://notifikasjon-fake-produsent-api.ekstern.dev.nav.no/api/graphql"
        val token = ""
        val arbeidsgiverNotifikasjonKlient = ArbeidsgiverNotifikasjonKlient(url) { token }
        testSuspend {
            val result = arbeidsgiverNotifikasjonKlient.whoami()
            println(result)
        }
    }
}

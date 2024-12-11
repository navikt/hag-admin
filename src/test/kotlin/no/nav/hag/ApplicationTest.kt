package no.nav.hag

import com.nimbusds.jwt.SignedJWT
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.ktor.test.dispatcher.testSuspend
import no.nav.hag.plugins.configureRouting
import no.nav.hag.plugins.configureSecurity
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {

        val server = MockOAuth2Server()
        server.start(port = 6666)
        val issuerId = "employee"
        application {
            configureSecurity(disabled = false)
            configureRouting(FakeServiceImpl())
        }
        val token: SignedJWT = server.issueToken(issuerId, Env.oauth2Environment.clientId, DefaultOAuth2TokenCallback(subject = "user123", audience = listOf(Env.oauth2Environment.clientId), claims = mapOf("NAVident" to "User123")))
        println("Token: ${token.serialize()}")
        client.get("/") {
            headers {
                append("Authorization", "Bearer ${token.serialize()}")
            }
        }.apply {
            println(bodyAsText())
            assertEquals(HttpStatusCode.OK, status)
        }
        client.get("/admin-ui/ferdigstillOppgave-form.html").apply {
            assertEquals(HttpStatusCode.OK, status)

        }
        server.shutdown()
    }

    @Test
    fun testKlient() {
        val url ="https://notifikasjon-fake-produsent-api.ekstern.dev.nav.no/api/graphql"
        val token = ""
        val arbeidsgiverNotifikasjonKlient = ArbeidsgiverNotifikasjonKlient(url, {token})
        testSuspend {
            val result =  arbeidsgiverNotifikasjonKlient.whoami()
            println(result)
        }
    }
}

package no.nav.hag

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*
import no.nav.hag.plugins.*
import java.util.UUID

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            configureRouting(FakeServiceImpl())
        }
        client.get("/admin-ui/ferdigstillOppgave-form.html").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        val uuid = UUID.randomUUID()
        println(uuid)
    }
}

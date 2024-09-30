package no.nav.hag

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.hag.plugins.*
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.tokenprovider.oauth2ClientCredentialsTokenGetter

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {

    configureSecurity()
    val service = when {
        Env.isTestMode() -> FakeServiceImpl()
        else -> NotifikasjonServiceImpl()
    }
    configureRouting(service)

}



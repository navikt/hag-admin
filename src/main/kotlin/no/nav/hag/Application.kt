package no.nav.hag

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics
import kotlinx.serialization.json.Json
import no.nav.hag.plugins.configureRouting
import no.nav.hag.plugins.configureSecurity
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import org.slf4j.LoggerFactory

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    JvmMetrics.builder().register()
    val logger = LoggerFactory.getLogger(Application::class.java)
    val isTestMode = Env.isTest()
    val authClient = AuthClient()

    logger.info("Started App - testmode = $isTestMode")

    configureSecurity(authClient, disabled = isTestMode)
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
            },
        )
    }
    val tokenGetter = authClient.tokenGetter(Env.agNotifikasjonScope)
    val agNotifikasjonKlient = ArbeidsgiverNotifikasjonKlient(Env.agNotifikasjonUrl, tokenGetter)

    val service =
        when {
            Env.isLocal() -> FakeServiceImpl()
            else -> NotifikasjonServiceImpl(agNotifikasjonKlient, Env.utgaattUrl)
        }
    configureRouting(service)
}

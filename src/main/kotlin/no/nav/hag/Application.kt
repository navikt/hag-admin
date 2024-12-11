package no.nav.hag

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.hag.plugins.configureRouting
import no.nav.hag.plugins.configureSecurity
import org.slf4j.LoggerFactory

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val logger = LoggerFactory.getLogger(Application::class.java)
    val isTestMode = Env.isTest()
    logger.info("Started App - testmode = $isTestMode")
    configureSecurity(disabled = isTestMode)
    val service = when {
        Env.isLocal() -> FakeServiceImpl()
        else -> NotifikasjonServiceImpl()
    }
    configureRouting(service)
}



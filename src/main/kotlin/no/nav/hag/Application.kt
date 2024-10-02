package no.nav.hag

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.hag.plugins.*
import org.slf4j.LoggerFactory

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val logger = LoggerFactory.getLogger(Application::class.java)
    logger.info("Started: Testmode = ${Env.isTestMode()}")
    configureSecurity()
    val service = when {
        Env.isTestMode() -> FakeServiceImpl()
        else -> NotifikasjonServiceImpl()
    }
    configureRouting(service)

}



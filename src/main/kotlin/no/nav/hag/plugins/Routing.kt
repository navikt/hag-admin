package no.nav.hag.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.authorization
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import no.nav.hag.ForespoerselService
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.util.UUID

fun Application.configureRouting(
    forespoerselService: ForespoerselService,
    appMicrometerRegistry: PrometheusMeterRegistry,
) {
    routing {
        get("/metrics") {
            call.respond(appMicrometerRegistry.scrape())
        }
        authenticate {
            staticResources("/", "frontend")
            get("/hentBrukernavn") {
                call.respondText(hentBrukernavnFraToken())
            }
            post("/hentEksponerteForespoersler") {
                val request =
                    try {
                        call.receive<HentEksponerteForespoerslerRequest>()
                    } catch (e: BadRequestException) {
                        call.respond(HttpStatusCode.BadRequest, "Ugyldig JSON-body")
                        return@post
                    }
                if (request.fnr.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "fnr må være utfylt")
                    return@post
                }
                try {
                    val forespoersler = forespoerselService.hentEksponerteForespoersler(request.fnr)
                    call.respond(HttpStatusCode.OK, JsonArray(forespoersler))
                } catch (ex: Exception) {
                    sikkerLogger().error("Feil ved henting av eksponerte forespørsler: ${ex.message}", ex)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        "Feil ved henting av eksponerte forespørsler, prøv igjen senere",
                    )
                }
            }
            post("/forkastForespoersel") {
                val forkastForespoerselRequest = call.receive<ForkastForespoerselRequest>()
                if (forkastForespoerselRequest.forespoerselId.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "forespoerselId må være utfylt")
                    return@post
                }
                val forespoerselId =
                    try {
                        UUID.fromString(forkastForespoerselRequest.forespoerselId)
                    } catch (_: IllegalArgumentException) {
                        call.respond(HttpStatusCode.BadRequest, "forespoerselId må være en gyldig UUID")
                        return@post
                    }
                try {
                    forespoerselService.forkastForespoerselOgVentPaaBekreftelse(
                        forespoerselId,
                        hentBrukernavnFraToken(),
                    )
                    call.respond(HttpStatusCode.OK)
                } catch (ex: Exception) {
                    sikkerLogger().error("Feil ved forkasting av forespørsel: ${ex.message}", ex)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        "Feil ved forkasting av forespørsel, prøv igjen senere",
                    )
                }
            }
        }
    }
}

@Serializable
internal data class HentEksponerteForespoerslerRequest(
    val fnr: String,
)

@Serializable
internal data class ForkastForespoerselRequest(
    val forespoerselId: String,
)

private fun RoutingContext.hentBrukernavnFraToken(): String =
    call.request
        .authorization()
        .readClaim("NAVident")
        ?.asString()
        ?: "Ukjent bruker"


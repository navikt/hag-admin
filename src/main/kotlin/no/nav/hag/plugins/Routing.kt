package no.nav.hag.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.hag.NotifikasjonService
import java.util.UUID

fun Application.configureRouting(notifikasjonService: NotifikasjonService) {
    routing {
        staticResources("/admin-ui", "admin-ui")
        authenticate {
            get("/") {
                call.respondText("HAG Admin Tool")
            }
            post("/ferdigstillOppgave") {
                val skjema = call.receiveParameters()
                val oppgaveId = skjema["oppgaveId"]
                if (oppgaveId.isNullOrEmpty()) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }
                try {
                    UUID.fromString(oppgaveId)
                    notifikasjonService.ferdigstillOppgave(oppgaveId)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                } catch (ex: Exception) {
                    call.respond(HttpStatusCode.InternalServerError)
                }
                call.respond(HttpStatusCode.OK)
            }
        }
    }

}


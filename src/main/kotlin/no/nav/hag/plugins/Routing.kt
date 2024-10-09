package no.nav.hag.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.html.respondHtml
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.h2
import kotlinx.html.p
import no.nav.hag.NotifikasjonService
import org.slf4j.LoggerFactory
import java.util.UUID

fun Application.configureRouting(notifikasjonService: NotifikasjonService) {

    val logger = LoggerFactory.getLogger(Routing::class.java)

    routing {
        staticResources("/admin-ui", "admin-ui")
        authenticate {
            get("/") {
                call.respondHtml(HttpStatusCode.OK) {
                    body {
                        h2 {
                            +"HAG Admin Tool"
                        }
                        p {
                            a(href = "admin-ui/ferdigstillOppgave-form.html") {
                                +"Ferdigstill oppgave"
                            }
                        }
                        p {
                            a(href = "admin-ui/softDeleteSak-form.html") {
                                +"Slett sak"
                            }
                        }
                    }
                }
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
                call.respondHtml(HttpStatusCode.OK) {
                    body {
                        h2 {
                            +"Utført OK"
                        }
                    }
                }
            }
            post("/slettSak") {
                val skjema = call.receiveParameters()
                val foresporselId = skjema["foresporselId"]
                if (foresporselId.isNullOrEmpty()) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }
                try {
                    UUID.fromString(foresporselId)
                    notifikasjonService.slettSak(foresporselId)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                } catch (ex: Exception) {
                    call.respond(HttpStatusCode.InternalServerError)
                }
                call.respondHtml(HttpStatusCode.OK) {
                    body {
                        h2 {
                            +"Utført OK"
                        }
                    }
                }
            }
        }
    }

}


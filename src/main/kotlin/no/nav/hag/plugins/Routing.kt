package no.nav.hag.plugins

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.html.respondHtml
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.authorization
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.css.Color
import kotlinx.css.CssBuilder
import kotlinx.css.Margin
import kotlinx.css.backgroundColor
import kotlinx.css.body
import kotlinx.css.margin
import kotlinx.css.px
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.h2
import kotlinx.html.head
import kotlinx.html.link
import kotlinx.html.p
import no.nav.hag.Env
import no.nav.hag.NotifikasjonService
import no.nav.hag.domain.ForespoerselBatchSletter
import no.nav.helsearbeidsgiver.utils.log.logger
import java.util.UUID

fun Application.configureRouting(notifikasjonService: NotifikasjonService) {

    routing {
        staticResources("/admin-ui", "admin-ui")
        get("/styles.css") {
            call.respondCss {
                body {
                    if (Env.isTest()) {
                        backgroundColor = Color.limeGreen
                    } else {
                        backgroundColor = Color.red
                    }
                    margin = Margin(0.px)
                }
            }
        }
        authenticate {
            get("/") {
                val brukernavn = hentBrukernavnFraToken()
                call.respondHtml(HttpStatusCode.OK) {
                    head {
                        link(rel = "stylesheet", href = "/styles.css", type = "text/css")
                    }
                    body {
                        h2 {
                            +"HAG Admin Tool"
                        }
                        p {
                            text(
                                "Logget inn som: $brukernavn"
                            )
                        }
                        p {
                            a(href = "admin-ui/ferdigstillOppgave-form.html") {
                                +"Ferdigstill oppgave"
                            }
                        }
                        p {
                            a(href = "admin-ui/hardDeleteSak-form.html") {
                                +"Slett sak"
                            }
                        }
                        p {
                            a(href = "admin-ui/hardDeleteSaker-form.html") {
                                +"Slett saker (Batch)"
                            }
                        }
                    }
                }
            }
            post("/ferdigstillOppgave") {
                val skjema = call.receiveParameters()
                val foresporselId = skjema["foresporselId"]
                if (foresporselId.isNullOrEmpty()) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }
                try {
                    val brukernavn = hentBrukernavnFraToken()
                    UUID.fromString(foresporselId)
                    notifikasjonService.ferdigstillOppgave(foresporselId, brukernavn)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                } catch (ex: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ex.message.toString())
                }
                call.respondHtml(HttpStatusCode.OK) {
                    head {
                        link(rel = "stylesheet", href = "/styles.css", type = "text/css")
                    }
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
                    val brukernavn = hentBrukernavnFraToken()
                    UUID.fromString(foresporselId)
                    notifikasjonService.slettSak(foresporselId, brukernavn)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                } catch (ex: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ex.message.toString())
                }
                call.respondHtml(HttpStatusCode.OK) {
                    head {
                        link(rel = "stylesheet", href = "/styles.css", type = "text/css")
                    }
                    body {
                        h2 {
                            +"Utført OK"
                        }
                    }
                }
            }
            post("/slettSaker") {
                val skjema = call.receiveParameters()
                val foresporselIdInput = skjema["foresporselIdInput"]
                if (foresporselIdInput.isNullOrEmpty()) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }
                try {
                    val brukernavn = hentBrukernavnFraToken()
                    val forespoerselSletter = ForespoerselBatchSletter(notifikasjonService, brukernavn)
                    val rapport = forespoerselSletter.slett(foresporselIdInput)
                    logger().info(rapport.toString())
                    call.respond(HttpStatusCode.OK, rapport)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest,"Ugyldig input: ${e.message}")
                    return@post
                } catch (ex: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ex.message.toString())
                }

            }
        }
    }
}

private fun RoutingContext.hentBrukernavnFraToken(): String =
    call.request
        .authorization()
        .readClaim("NAVident")
        ?.asString()
        ?: "Ukjent bruker"

suspend inline fun ApplicationCall.respondCss(builder: CssBuilder.() -> Unit) {
    this.respondText(CssBuilder().apply(builder).toString(), ContentType.Text.CSS)
}

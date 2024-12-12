package no.nav.hag.plugins

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.html.respondHtml
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.util.pipeline.PipelineContext
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
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import org.slf4j.LoggerFactory
import java.util.UUID

fun Application.configureRouting(notifikasjonService: NotifikasjonService) {

    val logger = LoggerFactory.getLogger(Routing::class.java)
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
                            a(href = "admin-ui/softDeleteSak-form.html") {
                                +"Slett sak"
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
                    call.respond(HttpStatusCode.InternalServerError)
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
                    call.respond(HttpStatusCode.InternalServerError)
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
        }
    }
}

private fun PipelineContext<Unit, ApplicationCall>.hentBrukernavnFraToken(): String {
    val principal: TokenValidationContextPrincipal? = call.authentication.principal()
    return principal?.context?.getClaims("employee")?.getStringClaim("NAVident") ?: "Ukjent bruker"
}

suspend inline fun ApplicationCall.respondCss(builder: CssBuilder.() -> Unit) {
    this.respondText(CssBuilder().apply(builder).toString(), ContentType.Text.CSS)
}

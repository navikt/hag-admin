package no.nav.hag.plugins


import io.ktor.server.application.Application
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authentication
import io.ktor.server.auth.basic
import io.ktor.server.auth.bearer
import no.nav.hag.AuthClient
import no.nav.hag.Env

fun Application.configureSecurity(authClient: AuthClient, disabled: Boolean = false) {
    if (disabled && Env.isTest()) {
        authentication {
            basic {
                skipWhen { true }
            }
        }
    } else {
        authentication {
            bearer {
                authenticate {
                    if (authClient.introspect(it.token)) {
                        UserIdPrincipal("hag-admin")
                    } else {
                        null
                    }
                }
            }
        }
    }
}

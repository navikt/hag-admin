package no.nav.hag.plugins

import io.ktor.server.application.Application
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authentication
import io.ktor.server.auth.basic
import io.ktor.server.auth.bearer
import no.nav.hag.AuthClient
import no.nav.hag.Env

const val GROUP_ID_HAG = "dda4c4ed-1820-4b25-920f-eb2f54e19d8b"

fun Application.configureSecurity(
    authClient: AuthClient,
    disabled: Boolean = false,
) {
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
                    if (authClient.introspect(it.token) && it.token.containsClaimForAllowedGroup()) {
                        UserIdPrincipal("hag-admin")
                    } else {
                        null
                    }
                }
            }
        }
    }
}

private fun String.containsClaimForAllowedGroup(): Boolean =
    readClaim("groups")
        ?.asList(String::class.java)
        .orEmpty()
        .contains(GROUP_ID_HAG)

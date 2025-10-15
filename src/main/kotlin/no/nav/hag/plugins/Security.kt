package no.nav.hag.plugins

import io.ktor.server.application.Application
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authentication
import io.ktor.server.auth.basic
import io.ktor.server.auth.bearer
import no.nav.hag.AuthClient
import no.nav.hag.Env

const val GROUP_ID_HAG = "e3ab1801-e5a6-48ca-9c3b-5a91ce182c57"

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

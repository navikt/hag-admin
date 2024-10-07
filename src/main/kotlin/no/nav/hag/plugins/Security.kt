package no.nav.hag.plugins


import io.ktor.server.application.*
import io.ktor.server.auth.*
import no.nav.hag.Env
import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.TokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport

fun Application.configureSecurity(disabled: Boolean = false) {
    if (disabled && Env.isTest()) {
        authentication {
            basic {
                skipWhen { true }
            }
        }
    } else {
        val config =
            TokenSupportConfig(
                IssuerConfig(
                    name = "employee",
                    discoveryUrl = Env.oauth2Environment.wellKnownUrl,
                    acceptedAudience = listOf(Env.oauth2Environment.clientId),
                )
            )
        authentication {
            tokenValidationSupport(
                config = config,
            )
        }
    }

}

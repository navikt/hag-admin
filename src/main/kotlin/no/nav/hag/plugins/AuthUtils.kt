package no.nav.hag.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.Claim
import io.ktor.http.auth.AuthScheme

fun String?.readClaim(name: String): Claim? =
    this?.removePrefix("${AuthScheme.Bearer} ")
        ?.let(JWT::decode)
        ?.getClaim(name)

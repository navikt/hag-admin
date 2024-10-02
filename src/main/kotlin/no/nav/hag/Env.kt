package no.nav.hag

import no.nav.helsearbeidsgiver.tokenprovider.OAuth2Environment
object Env {
    val clusterName = System.getenv("NAIS_CLUSTER_NAME")
    val notifikasjonUrl = System.getenv("ARBEIDSGIVER_NOTIFIKASJON_API_URL")
    val oauth2Environment =
        OAuth2Environment(
            scope = System.getenv("ARBEIDSGIVER_NOTIFIKASJON_SCOPE") ?: "",
            wellKnownUrl = System.getenv("AZURE_APP_WELL_KNOWN_URL") ?: "",
            tokenEndpointUrl = System.getenv("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT") ?: "",
            clientId = System.getenv("AZURE_APP_CLIENT_ID") ?: "",
            clientSecret = System.getenv("AZURE_APP_CLIENT_SECRET") ?: "",
            clientJwk = System.getenv("AZURE_APP_JWK") ?: "",
        )

    fun isLocal() : Boolean {
        return clusterName == null
    }
}
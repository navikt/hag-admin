package no.nav.hag

import no.nav.helsearbeidsgiver.tokenprovider.OAuth2Environment
object Env {
    val clusterName = System.getenv("NAIS_CLUSTER_NAME")
    val notifikasjonUrl = System.getenv("ARBEIDSGIVER_NOTIFIKASJON_API_URL")
    val oauth2Environment =
        OAuth2Environment(
            scope = System.getenv("ARBEIDSGIVER_NOTIFIKASJON_SCOPE") ?: "dummyscope",
            wellKnownUrl = System.getenv("AZURE_APP_WELL_KNOWN_URL") ?: "http://localhost:6666/employee/.well-known/openid-configuration",
            tokenEndpointUrl = System.getenv("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT") ?: "http://localhost:6666/",
            clientId = System.getenv("AZURE_APP_CLIENT_ID") ?: "123",
            clientSecret = System.getenv("AZURE_APP_CLIENT_SECRET") ?: "123",
            clientJwk = System.getenv("AZURE_APP_JWK") ?: "",
        )

    fun isTest() : Boolean {
        return isLocal() || clusterName == "dev-gcp"
    }

    fun isLocal() : Boolean {
        return clusterName == null
    }
}
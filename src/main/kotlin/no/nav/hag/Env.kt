package no.nav.hag

import no.nav.helsearbeidsgiver.tokenprovider.OAuth2Environment
object Env {
    val defaultClusterName = "dummy"
    val clusterName = System.getProperty("NAIS_CLUSTER_NAME", defaultClusterName)
    val notifikasjonUrl = System.getProperty("ARBEIDSGIVER_NOTIFIKASJON_API_URL", "dummyUrl")
    val oauth2Environment =
        OAuth2Environment(
            scope = System.getProperty("ARBEIDSGIVER_NOTIFIKASJON_SCOPE", "dummyScope"),
            wellKnownUrl = System.getProperty("AZURE_APP_WELL_KNOWN_URL", "dummyWellknownUrl"),
            tokenEndpointUrl = System.getProperty("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT", "dummyEndpoint"),
            clientId = System.getProperty("AZURE_APP_CLIENT_ID", "dummyClientId"),
            clientSecret = System.getProperty("AZURE_APP_CLIENT_SECRET", "dummyClientSecret"),
            clientJwk = System.getProperty("AZURE_APP_JWK", "dummyJWK"),
        )

    fun isTestMode() : Boolean {
        return clusterName == defaultClusterName
    }
}
package no.nav.hag

object Env {
    private val clusterName = System.getenv("NAIS_CLUSTER_NAME")

    val agNotifikasjonScope = System.getenv("ARBEIDSGIVER_NOTIFIKASJON_SCOPE") ?: "dummyscope"
    val agNotifikasjonUrl = System.getenv("ARBEIDSGIVER_NOTIFIKASJON_API_URL") ?: "http://dummy"
    val utgaattUrl = System.getenv("SAK_UTGAATT_URL") ?: "http://expired" // link til statisk "Nav trenger ikke IM"-side. Brukes ved "ferdigstill sak"

    fun isTest(): Boolean {
        return isLocal() || clusterName == "dev-gcp"
    }

    fun isLocal(): Boolean {
        return clusterName == null
    }
}

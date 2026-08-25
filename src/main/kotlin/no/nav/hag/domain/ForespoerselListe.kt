package no.nav.hag.domain

import java.util.UUID

class ForespoerselListe(
    val liste: String,
) {
    /*
    Tar imot en String med UUIDer separert med linjeskift. lines() takler CRLF, LF og CR
    Fjerner whitespace og returnerer et map med orginalString som peker på UUID, null hvis ugyldig UUID
     */
    fun konverterInput(): Map<String, UUID?> {
        if (liste.isBlank()) return emptyMap()
        return liste
            .lines()
            .map {
                it.trim()
            }.filter { it.isNotBlank() }
            .associateWith { parseUUID(it) }
    }

    fun konverterTilNotifikasjonData(): Map<String, String> {
        if (liste.isBlank()) return emptyMap()
        return liste
            .lines()
            .map {
                it.trim()
            }.filter { it.isNotBlank() }
            .map { it.split(",") }
            .associate { Pair(it[0], it[1]) }
    }

    private fun parseUUID(element: String): UUID? {
        try {
            return UUID.fromString(element)
        } catch (e: IllegalArgumentException) {
            return null
        }
    }
}

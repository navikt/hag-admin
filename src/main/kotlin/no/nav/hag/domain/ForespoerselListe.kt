package no.nav.hag.domain

import java.util.UUID

class ForespoerselListe(val liste: String) {

    /*
    Tar imot en String med UUIDer separert med semikolon.
    Fjerner whitespace og returnerer et map med orginalString som peker på UUID, null hvis ugyldig UUID
     */
    fun konverterInput(): Map<String, UUID?> {
        if (liste.isBlank()) return emptyMap()
        return liste.split(";").map {
            it.trim()
        }
            .filter { it.isNotBlank() }
            .associateWith { parseUUID(it) }
    }

    private fun parseUUID(element: String): UUID? {
        try {
            return UUID.fromString(element)
        } catch (e: IllegalArgumentException) {
            return null
        }
    }
}

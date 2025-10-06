package no.nav.hag.domain

import kotlinx.serialization.Serializable
import no.nav.hag.NotifikasjonService

@Serializable
enum class SletteStatus {
    OK,
    UGYLDIG,
    FEILET
}

@Serializable
data class SletteResultat(val uuid: String, val status: SletteStatus)

class ForespoerselBatchSletter(val notifikasjonService: NotifikasjonService, val brukernavn: String) {

    suspend fun slett(liste: String): List<SletteResultat> {
        val liste = ForespoerselListe(liste).konverterInput()
        val resultat = liste.map {
            if (it.value == null) {
                SletteResultat(it.key, SletteStatus.UGYLDIG)
            } else {
                try {
                    notifikasjonService.slettSak(it.key, brukernavn)
                    SletteResultat(it.key, SletteStatus.OK)
                } catch (e: Exception) {
                    SletteResultat(it.key , SletteStatus.FEILET)
                }
            }
        }
        return resultat
    }
}

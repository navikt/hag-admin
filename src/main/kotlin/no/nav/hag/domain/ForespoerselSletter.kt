package no.nav.hag.domain

import kotlinx.serialization.Serializable
import no.nav.hag.NotifikasjonService

@Serializable
enum class Status {
    OK,
    UGYLDIG,
    FEILET,
}

enum class Operasjon {
    SLETT,
    FERDIGSTILL_OPPGAVE,
    FERDIGSTILL_SAK,
}

@Serializable
data class Resultat(
    val uuid: String,
    val status: Status,
)

class NotifikasjonBatcher(
    val notifikasjonService: NotifikasjonService,
    val brukernavn: String,
) {
    suspend fun slettSaker(batch: String): List<Resultat> = utfoerBatchOperasjon(Operasjon.SLETT, batch)

    suspend fun ferdigstillOppgaver(batch: String): List<Resultat> = utfoerBatchOperasjon(Operasjon.FERDIGSTILL_OPPGAVE, batch)

    suspend fun ferdigstillSaker(batch: String): List<Resultat> = utfoerBatchOperasjon(Operasjon.FERDIGSTILL_SAK, batch)

    private suspend fun utfoerBatchOperasjon(
        operasjon: Operasjon,
        batch: String,
    ): List<Resultat> {
        val liste = ForespoerselListe(batch).konverterInput()
        val resultat =
            liste.map {
                if (it.value == null) {
                    Resultat(it.key, Status.UGYLDIG)
                } else {
                    try {
                        when (operasjon) {
                            Operasjon.FERDIGSTILL_OPPGAVE -> notifikasjonService.ferdigstillOppgave(it.key, brukernavn)
                            Operasjon.SLETT -> notifikasjonService.slettSak(it.key, brukernavn)
                            Operasjon.FERDIGSTILL_SAK -> notifikasjonService.ferdigstillSak(it.key, brukernavn)
                        }
                        Resultat(it.key, Status.OK)
                    } catch (e: Exception) {
                        Resultat(it.key, Status.FEILET)
                    }
                }
            }
        return resultat
    }
}

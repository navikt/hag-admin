package no.nav.hag.domain

import kotlinx.serialization.Serializable
import no.nav.hag.NotifikasjonService
import no.nav.helsearbeidsgiver.brreg.BrregClient
import no.nav.helsearbeidsgiver.utils.collection.mapValuesNotNull

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
    val brregClient: BrregClient,
) {
    suspend fun slettSaker(batch: String): List<Resultat> = utfoerBatchOperasjon(Operasjon.SLETT, batch)

    suspend fun ferdigstillOppgaver(batch: String): List<Resultat> = utfoerBatchOperasjon(Operasjon.FERDIGSTILL_OPPGAVE, batch)

    suspend fun nyPaaminnelse(batch: String): List<Resultat> = oppdaterNotifikasjoner(batch)

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

    private suspend fun oppdaterNotifikasjoner(batch: String): List<Resultat> {
        val liste = ForespoerselListe(batch).konverterTilNotifikasjonData()
        val orgnumre =
            brregClient
                .hentOrganisasjonNavn(liste.mapValuesNotNull { it }.values.toSet())
                .map {
                    it.key.verdi to it.value
                }.toMap()
        val alleData = slaaSammen(liste, orgnumre)
        val resultat =
            alleData.map {
                try {
                    notifikasjonService.lagNyPaaminnelse(it, brukernavn)
                    Resultat(it.forespoerselId, Status.OK)
                } catch (e: Exception) {
                    Resultat(it.forespoerselId, Status.FEILET)
                }
            }
        return resultat
    }

    private fun slaaSammen(
        liste: Map<String, String>, // ForespørselId, Orgnr som String
        orgnumre: Map<String, String>, // Orgnr, Navn
    ): List<ForespoerselData> =
        liste.map {
            ForespoerselData(it.key, it.value, orgnumre[it.value] ?: "")
        }
}

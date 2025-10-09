package no.nav.hag

import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.arbeidsgivernotifkasjon.graphql.generated.enums.SaksStatus
import org.slf4j.LoggerFactory


interface NotifikasjonService {
    suspend fun ferdigstillOppgave(foresporselId: String, brukernavn: String)
    suspend fun ferdigstillSak(foresporselId: String, brukernavn: String)
    suspend fun slettSak(foresporselId: String, brukernavn: String)
}

class NotifikasjonServiceImpl(notifikasjonKlient: ArbeidsgiverNotifikasjonKlient, val utgaattUrl: String) : NotifikasjonService {
    val logger = LoggerFactory.getLogger(NotifikasjonServiceImpl::class.java)
    val merkelapp = "Inntektsmelding sykepenger"
    val klient = notifikasjonKlient

    override suspend fun ferdigstillOppgave(foresporselId: String, brukernavn: String) {
        logger.info("Ferdigstiller oppgave for forespørsel: $foresporselId. Utført av $brukernavn")
        runCatching {
            klient.oppgaveUtgaattByEksternId(
                eksternId = foresporselId,
                merkelapp = merkelapp,
            )
        }.onFailure { error ->
            logger.error("Fant ikke oppgave under endring til utgått.", error)
            throw error
        }
    }

    override suspend fun ferdigstillSak(foresporselId: String, brukernavn: String) {
        logger.info("Ferdigstiller sak for forespørsel: $foresporselId. Utført av $brukernavn")
        runCatching {
            klient.nyStatusSakByGrupperingsid(
                grupperingsid = foresporselId,
                merkelapp = merkelapp,
                status = SaksStatus.FERDIG,
                nyLenke = utgaattUrl,
            )
        }.onFailure { error ->
            logger.error("Fant ikke sak under ferdigstilling.", error)
            throw error
        }
    }

    override suspend fun slettSak(foresporselId: String, brukernavn: String) {
        logger.info("Sletter sak for forespørsel $foresporselId. Utført av $brukernavn")
        runCatching {
            klient.hardDeleteSakByGrupperingsid(
                grupperingsid = foresporselId,
                merkelapp = merkelapp
            )
        }.onFailure { error ->
            logger.error("Klarte ikke å slette sak", error)
            throw error
        }
    }

}

class FakeServiceImpl : NotifikasjonService {
    val logger = LoggerFactory.getLogger(FakeServiceImpl::class.java)

    override suspend fun ferdigstillOppgave(foresporselId: String, brukernavn: String) {
        logger.info("Bruker: $brukernavn ferdigstilte oppgave for forespørselId: $foresporselId")
    }

    override suspend fun ferdigstillSak(foresporselId: String, brukernavn: String) {
        logger.info("Bruker: $brukernavn ferdigstilte sak for forespørselId: $foresporselId")
    }

    override suspend fun slettSak(foresporselId: String, brukernavn: String) {
        logger.info("Bruker: $brukernavn slettet sak for forespørselId: $foresporselId")
    }
}

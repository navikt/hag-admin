package no.nav.hag

import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.OppgaveUtgaattByEksternIdException
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.SoftDeleteSakByGrupperingsidException
import org.slf4j.LoggerFactory


interface NotifikasjonService {
    suspend fun ferdigstillOppgave(foresporselId: String, brukernavn: String)
    suspend fun slettSak(foresporselId: String, brukernavn: String)
}

class NotifikasjonServiceImpl(notifikasjonKlient: ArbeidsgiverNotifikasjonKlient) : NotifikasjonService {
    val logger = LoggerFactory.getLogger(NotifikasjonServiceImpl::class.java)
    val merkelapp = "Inntektsmelding sykepenger"
    val gammel_merkelapp ="Inntektsmelding"
    val klient = notifikasjonKlient

    override suspend fun ferdigstillOppgave(foresporselId: String, brukernavn: String) {
        logger.info("Ferdigstiller oppgave for forespørsel: $foresporselId. Utført av $brukernavn")
        try {
            klient.oppgaveUtgaattByEksternId(merkelapp, foresporselId)
        } catch (e : OppgaveUtgaattByEksternIdException) {
            logger.info("Feil oppstod, forsøker heller å ferdigstille oppgave med gammel merkelapp")
            klient.oppgaveUtgaattByEksternId(gammel_merkelapp, foresporselId)
        }
    }

    override suspend fun slettSak(foresporselId: String, brukernavn: String) {
        logger.info("Sletter sak for forespørsel $foresporselId. Utført av $brukernavn")
        try {
            klient.softDeleteSakByGrupperingsid(foresporselId, merkelapp)
        } catch (e : SoftDeleteSakByGrupperingsidException) {
            logger.info("Feil oppstod, forsøker heller å slette sak med gammel merkelapp")
            klient.softDeleteSakByGrupperingsid(foresporselId, gammel_merkelapp)
        }
    }

}

class FakeServiceImpl : NotifikasjonService {
    override suspend fun ferdigstillOppgave(foresporselId: String, brukernavn: String) {
        val logger = LoggerFactory.getLogger(FakeServiceImpl::class.java)
        logger.info("Bruker: $brukernavn ferdigstilt oppgave for forespørselId: $foresporselId")
    }

    override suspend fun slettSak(foresporselId: String, brukernavn: String) {
        TODO("Not yet implemented")
    }
}

package no.nav.hag

import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.tokenprovider.oauth2ClientCredentialsTokenGetter
import org.slf4j.LoggerFactory


interface NotifikasjonService {
    suspend fun ferdigstillOppgave(foresporselId: String, brukernavn: String)
    suspend fun slettSak(foresporselId: String, brukernavn: String)
}

class NotifikasjonServiceImpl : NotifikasjonService {
    val logger = LoggerFactory.getLogger(NotifikasjonServiceImpl::class.java)
    val klient = buildNotifikasjonKlient()
    val merkelapp = "Inntektsmelding sykepenger"

    override suspend fun ferdigstillOppgave(foresporselId: String, brukernavn: String) {
        logger.info("Ferdigstiller oppgave for forespørsel: $foresporselId. Utført av $brukernavn")
        klient.oppgaveUtgaattByEksternId(merkelapp, foresporselId)
    }

    override suspend fun slettSak(foresporselId: String, brukernavn: String) {
        logger.info("Sletter sak for forespørsel $foresporselId. Utført av $brukernavn")
        klient.softDeleteSakByGrupperingsid(foresporselId, merkelapp)
    }

    private fun buildNotifikasjonKlient(): ArbeidsgiverNotifikasjonKlient {
        val tokenGetter = oauth2ClientCredentialsTokenGetter(Env.oauth2Environment)
        return ArbeidsgiverNotifikasjonKlient(Env.notifikasjonUrl, tokenGetter)
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

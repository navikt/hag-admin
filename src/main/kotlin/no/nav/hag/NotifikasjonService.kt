package no.nav.hag

import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.tokenprovider.oauth2ClientCredentialsTokenGetter
import org.slf4j.LoggerFactory


interface NotifikasjonService {
    suspend fun ferdigstillOppgave(oppgaveId: String)
    suspend fun slettSak(foresporselId: String)
}

class NotifikasjonServiceImpl : NotifikasjonService {
    val logger = LoggerFactory.getLogger(NotifikasjonServiceImpl::class.java)
    val klient = buildNotifikasjonKlient()
    val merkelapp = "Inntektsmelding sykepenger"

    override suspend fun ferdigstillOppgave(oppgaveId: String) {
        logger.info("Ferdigstiller oppgave $oppgaveId")
        klient.oppgaveUtgaatt(oppgaveId)
    }

    override suspend fun slettSak(foresporselId: String) {
        logger.info("Sletter sak for forespørsel $foresporselId")
        klient.softDeleteSakByGrupperingsid(foresporselId, merkelapp)
    }

    private fun buildNotifikasjonKlient(): ArbeidsgiverNotifikasjonKlient {
        val tokenGetter = oauth2ClientCredentialsTokenGetter(Env.oauth2Environment)
        return ArbeidsgiverNotifikasjonKlient(Env.notifikasjonUrl, tokenGetter)
    }
}

class FakeServiceImpl : NotifikasjonService {
    override suspend fun ferdigstillOppgave(oppgaveId: String) {
        val logger = LoggerFactory.getLogger(FakeServiceImpl::class.java)
        logger.info("ferdigstilt oppgave: $oppgaveId")
    }

    override suspend fun slettSak(foresporselId: String) {
        TODO("Not yet implemented")
    }
}
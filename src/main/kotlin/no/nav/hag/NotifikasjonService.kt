package no.nav.hag

import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.arbeidsgivernotifkasjon.graphql.generated.enums.SaksStatus
import no.nav.helsearbeidsgiver.tokenprovider.oauth2ClientCredentialsTokenGetter
import org.slf4j.LoggerFactory


interface NotifikasjonService {
    suspend fun ferdigstillOppgave(oppgaveId: String)
}

class NotifikasjonServiceImpl : NotifikasjonService {
    val logger = LoggerFactory.getLogger(NotifikasjonServiceImpl::class.java)
    val klient = buildNotifikasjonKlient()

    override suspend fun ferdigstillOppgave(oppgaveId: String) {
        logger.info("Ferdigstiller oppgave $oppgaveId")
       // klient.oppgaveUtfoert(oppgaveId)
        logger.info("Logget inn som: ${klient.whoami()}")
        //klient.nyStatusSak(oppgaveId, SaksStatus.FERDIG, "Utgått")
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
}
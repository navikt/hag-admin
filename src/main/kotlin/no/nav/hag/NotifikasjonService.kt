package no.nav.hag

import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.tokenprovider.oauth2ClientCredentialsTokenGetter

interface NotifikasjonService {
    suspend fun ferdigstillOppgave(oppgaveId: String)
}

class NotifikasjonServiceImpl : NotifikasjonService {
    val klient = buildNotifikasjonKlient()

    override suspend fun ferdigstillOppgave(oppgaveId: String) {
        klient.oppgaveUtfoert(oppgaveId)
    }
    private fun buildNotifikasjonKlient(): ArbeidsgiverNotifikasjonKlient {
        val tokenGetter = oauth2ClientCredentialsTokenGetter(Env.oauth2Environment)
        return ArbeidsgiverNotifikasjonKlient(Env.notifikasjonUrl, tokenGetter)
    }
}

class FakeServiceImpl : NotifikasjonService {
    override suspend fun ferdigstillOppgave(oppgaveId: String) {
        println("ferdigstilt oppgave: $oppgaveId")
    }
}
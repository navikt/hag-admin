package no.nav.hag

import io.ktor.test.dispatcher.testSuspend
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.OppgaveUtgaattByEksternIdException
import kotlin.test.Test

class NotifikasjonServiceImplTest {

    @Test
    fun `notifikasjonService forsøker med gammel merkelapp dersom et kall feiler`() {

        val notifikasjonKlient = mockk<ArbeidsgiverNotifikasjonKlient>()
        val notifikasjonServiceImpl = NotifikasjonServiceImpl(notifikasjonKlient)

        coEvery { notifikasjonKlient.oppgaveUtgaattByEksternId(notifikasjonServiceImpl.merkelapp, any(), any()) } throws OppgaveUtgaattByEksternIdException("hei", "hå")
        coEvery { notifikasjonKlient.oppgaveUtgaattByEksternId(notifikasjonServiceImpl.gammel_merkelapp, any(), any()) } just runs

        testSuspend {
            notifikasjonServiceImpl.ferdigstillOppgave("123", "abc")
        }

        coVerify (exactly = 1) { notifikasjonKlient.oppgaveUtgaattByEksternId(notifikasjonServiceImpl.gammel_merkelapp, "123", any())}
    }

}

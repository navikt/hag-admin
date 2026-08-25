package no.nav.hag.domain

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.hag.FakeServiceImpl
import no.nav.helsearbeidsgiver.brreg.BrregClient
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID
import kotlin.test.Test

class NotifikasjonBatcherTest {
    @Test
    fun nyPaaminnelse() {
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        val orgnr1 = Orgnr.genererGyldig()
        val orgnr2 = Orgnr.genererGyldig()
        val brregClient = mockk<BrregClient>()
        coEvery { brregClient.hentOrganisasjonNavn(any()) } returns
            mapOf(
                orgnr1 to "Orgnummer1",
                orgnr2 to "Orgnummer2",
            )
        val input = uuid1.toString() + "," + orgnr1 + "\n" + uuid2 + "," + orgnr2
        val batcher = NotifikasjonBatcher(FakeServiceImpl(), "brukernavn", brregClient)
        runBlocking {
            batcher.nyPaaminnelse(
                input,
            )
        }
    }
}

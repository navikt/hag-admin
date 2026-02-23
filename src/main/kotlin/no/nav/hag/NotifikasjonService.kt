package no.nav.hag

import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.arbeidsgivernotifkasjon.graphql.generated.enums.SaksStatus
import no.nav.helsearbeidsgiver.arbeidsgivernotifkasjon.graphql.generated.hentsakmedgrupperingsid.Sak
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.days

interface NotifikasjonService {
    suspend fun ferdigstillOppgave(
        foresporselId: String,
        brukernavn: String,
    )

    suspend fun ferdigstillSak(
        foresporselId: String,
        brukernavn: String,
    )

    suspend fun slettSak(
        foresporselId: String,
        brukernavn: String,
    )

    suspend fun hentSak(foresporselId: String): Sak
}

class NotifikasjonServiceImpl(
    notifikasjonKlient: ArbeidsgiverNotifikasjonKlient,
    val utgaattUrl: String,
) : NotifikasjonService {
    private val logger = this::class.logger()
    private val sikkerLogger = sikkerLogger()
    private val merkelapp = "Inntektsmelding sykepenger"
    private val ferdigstiltSakLevetid = 90.days // Når en sak ferdigstilles, beholdes den i oversikten i 90 dager før sletting
    private val klient = notifikasjonKlient

    override suspend fun ferdigstillOppgave(
        foresporselId: String,
        brukernavn: String,
    ) {
        logger.info("Ferdigstiller oppgave for forespørsel: $foresporselId. Utført av $brukernavn")
        runCatching {
            klient.oppgaveUtgaattByEksternId(
                eksternId = foresporselId,
                merkelapp = merkelapp,
                nyLenke = utgaattUrl,
            )
        }.onFailure { error ->
            sikkerLogger.error("Fant ikke oppgave under endring til utgått.", error)
            logger.error("Fant ikke oppgave under endring til utgått.")
            throw error
        }
    }

    override suspend fun ferdigstillSak(
        foresporselId: String,
        brukernavn: String,
    ) {
        logger.info("Ferdigstiller sak for forespørsel: $foresporselId. Utført av $brukernavn")
        runCatching {
            klient.nyStatusSakByGrupperingsid(
                grupperingsid = foresporselId,
                merkelapp = merkelapp,
                status = SaksStatus.FERDIG,
                nyLenke = utgaattUrl,
                hardDeleteOm = ferdigstiltSakLevetid,
            )
        }.onFailure { error ->
            sikkerLogger.error("Fant ikke sak under ferdigstilling.", error)
            logger.error("Fant ikke sak under ferdigstilling.")
            throw error
        }
    }

    override suspend fun slettSak(
        foresporselId: String,
        brukernavn: String,
    ) {
        logger.info("Sletter sak for forespørsel $foresporselId. Utført av $brukernavn")
        runCatching {
            klient.hardDeleteSakByGrupperingsid(
                grupperingsid = foresporselId,
                merkelapp = merkelapp,
            )
        }.onFailure { error ->
            sikkerLogger.error("Klarte ikke å slette sak", error)
            logger.error("Klarte ikke å slette sak")
            throw error
        }
    }

    override suspend fun hentSak(foresporselId: String): Sak {
        val sak = klient.hentSakMedGrupperingsid(grupperingsid = foresporselId, merkelapp)
        sikkerLogger.info("Hentet sak: $sak")
        return sak
    }
}

class FakeServiceImpl : NotifikasjonService {
    val logger = LoggerFactory.getLogger(FakeServiceImpl::class.java)

    override suspend fun ferdigstillOppgave(
        foresporselId: String,
        brukernavn: String,
    ) {
        logger.info("Bruker: $brukernavn ferdigstilte oppgave for forespørselId: $foresporselId")
    }

    override suspend fun ferdigstillSak(
        foresporselId: String,
        brukernavn: String,
    ) {
        logger.info("Bruker: $brukernavn ferdigstilte sak for forespørselId: $foresporselId")
    }

    override suspend fun slettSak(
        foresporselId: String,
        brukernavn: String,
    ) {
        logger.info("Bruker: $brukernavn slettet sak for forespørselId: $foresporselId")
    }

    override suspend fun hentSak(foresporselId: String): Sak {
        TODO("Not yet implemented")
    }
}

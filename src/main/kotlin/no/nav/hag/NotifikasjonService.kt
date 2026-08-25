package no.nav.hag

import no.nav.hag.domain.ForespoerselData
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.Paaminnelse
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.Tjeneste
import no.nav.helsearbeidsgiver.arbeidsgivernotifkasjon.graphql.generated.enums.SaksStatus
import no.nav.helsearbeidsgiver.arbeidsgivernotifkasjon.graphql.generated.hentsakmedgrupperingsid.Sak
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.days

interface NotifikasjonService {
    suspend fun ferdigstillOppgave(
        foresporselId: String,
        brukernavn: String,
    )

    suspend fun lagNyPaaminnelse(
        foresporsel: ForespoerselData,
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
    private val ferdigstiltSakLevetid = 90.days // Når en sak ferdigstilles, beholdes den i oversikten i 90 dager før sletting
    private val klient = notifikasjonKlient

    override suspend fun ferdigstillOppgave(
        foresporselId: String,
        brukernavn: String,
    ) {
        logger.info("Ferdigstiller oppgave for forespørsel: $foresporselId. Utført av $brukernavn")
        runCatching {
            klient.oppgaveUtgaattByEksternId(
                tjeneste = Tjeneste.INNTEKTSMELDING,
                eksternId = foresporselId,
                nyLenke = utgaattUrl,
            )
        }.onFailure { error ->
            sikkerLogger.error("Fant ikke oppgave under endring til utgått.", error)
            logger.error("Fant ikke oppgave under endring til utgått.")
            throw error
        }
    }

    // Finner fager-oppgave og setter en ny påminnelse
    override suspend fun lagNyPaaminnelse(
        foresporsel: ForespoerselData,
        brukernavn: String,
    ) {
        logger.info("Lager ny påminnelse for oppgave for forespørsel: ${foresporsel.forespoerselId}. Utført av $brukernavn")
        runCatching {
            klient.endreOppgavePaaminnelserByEksternId(
                tjeneste = Tjeneste.INNTEKTSMELDING,
                eksternId = foresporsel.forespoerselId,
                paaminnelse =
                    Paaminnelse(
                        tittel = "Påminnelse – Vi mangler inntektsmelding for en av deres ansatte",
                        innhold = paaminnelseInnhold(foresporsel.orgnr, foresporsel.navn),
                        eksaktTid = LocalDateTime.now().plusMinutes(5),
                        tidMellomOppgaveopprettelseOgPaaminnelse = "", // Må settes, men brukes ikke når vi bruker eksaktTid
                    ),
            )
        }.onFailure { error ->
            sikkerLogger.error("Fant ikke oppgave.", error)
            logger.error("Fant ikke oppgave.")
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
                tjeneste = Tjeneste.INNTEKTSMELDING,
                grupperingsid = foresporselId,
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
                tjeneste = Tjeneste.INNTEKTSMELDING,
                grupperingsid = foresporselId,
            )
        }.onFailure { error ->
            sikkerLogger.error("Klarte ikke å slette sak", error)
            logger.error("Klarte ikke å slette sak")
            throw error
        }
    }

    override suspend fun hentSak(foresporselId: String): Sak {
        val sak = klient.hentSakMedGrupperingsid(grupperingsid = foresporselId, tjeneste = Tjeneste.INNTEKTSMELDING)
        sikkerLogger.info("Hentet sak: $sak")
        return sak
    }

    private fun paaminnelseInnhold(
        orgnr: String,
        orgNavn: String,
    ): String =
        listOf(
            "Nav har ennå ikke mottatt inntektsmeldingen for en av deres ansatte.",
            "For at vi skal kunne behandle søknaden om sykepenger, må inntektsmeldingen sendes inn så snart som mulig.",
            "Vennligst logg inn på Min side – arbeidsgiver hos Nav for å se hvilken inntektsmelding det gjelder.",
            "Arbeidsgiver: $orgNavn (orgnr $orgnr).",
        ).joinToString(separator = " ")
}

class FakeServiceImpl : NotifikasjonService {
    val logger = LoggerFactory.getLogger(FakeServiceImpl::class.java)

    override suspend fun ferdigstillOppgave(
        foresporselId: String,
        brukernavn: String,
    ) {
        logger.info("Bruker: $brukernavn ferdigstilte oppgave for forespørselId: $foresporselId")
    }

    override suspend fun lagNyPaaminnelse(
        foresporsel: ForespoerselData,
        brukernavn: String,
    ) {
        logger.info("Bruker: $brukernavn lagde ny påminnelse: $foresporsel")
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

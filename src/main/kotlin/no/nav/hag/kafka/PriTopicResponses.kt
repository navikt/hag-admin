package no.nav.hag.kafka

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

class PriTopicResponses {
    private val logger = this::class.logger()
    private val sikkerLogger = sikkerLogger()
    private val pending = ConcurrentHashMap<String, CompletableDeferred<List<JsonObject>>>()
    private val ventendeForkastinger = ConcurrentHashMap<UUID, CompletableDeferred<Unit>>()

    suspend fun forkastForespoersel(
        forespoerselId: UUID,
        sendRequest: suspend () -> Unit,
    ) {
        val response = CompletableDeferred<Unit>()
        check(ventendeForkastinger.putIfAbsent(forespoerselId, response) == null) {
            "Det finnes allerede en ventende forkasting for samme forespørsel."
        }
        try {
            sendRequest()
            logger.info("Venter på bekreftelse på forkasting, forespoerselId={}", forespoerselId)
            withTimeout(RESPONSE_TIMEOUT_MILLIS.milliseconds) { response.await() }
            logger.info("Forkasting bekreftet, forespoerselId={}", forespoerselId)
        } catch (e: TimeoutCancellationException) {
            logger.warn(
                "Tidsavbrudd ved forkasting etter {} ms, forespoerselId={}",
                RESPONSE_TIMEOUT_MILLIS,
                forespoerselId,
            )
            throw PriResponseTimeoutException()
        } catch (e: CancellationException) {
            logger.info("Venting på forkasting avbrutt, forespoerselId={}", forespoerselId)
            throw e
        } catch (e: Exception) {
            logger.error("Forkasting feilet, forespoerselId={}, feiltype={}", forespoerselId, e.javaClass.simpleName)
            sikkerLogger.error("Forkasting feilet, forespoerselId=$forespoerselId", e)
            throw e
        } finally {
            ventendeForkastinger.remove(forespoerselId, response)
        }
    }

    suspend fun request(
        fnr: String,
        sendRequest: () -> Unit,
    ): List<JsonObject> {
        val response = CompletableDeferred<List<JsonObject>>()
        val lookupId = UUID.randomUUID()
        check(pending.putIfAbsent(fnr, response) == null) {
            "HentFSPforPerson: Det finnes allerede en ventende request for samme person."
        }

        try {
            logger.info("HentFSPforPerson: sender oppslag, oppslagId={}", lookupId)
            sikkerLogger.info("HentFSPforPerson: sender oppslag, oppslagId={}, fnr={}", lookupId, fnr)
            sendRequest()
            logger.info("HentFSPforPerson: oppslag sendt, venter på svar, oppslagId={}", lookupId)
            return withTimeout(RESPONSE_TIMEOUT_MILLIS.milliseconds) { response.await() }.also {
                logger.info("HentFSPforPerson: oppslag fullført, oppslagId={}, antall={}", lookupId, it.size)
            }
        } catch (e: TimeoutCancellationException) {
            logger.warn("HentFSPforPerson: tidsavbrudd etter {} ms, oppslagId={}", RESPONSE_TIMEOUT_MILLIS, lookupId)
            sikkerLogger.warn("HentFSPforPerson: tidsavbrudd, oppslagId=$lookupId, fnr=$fnr", e)
            throw PriResponseTimeoutException()
        } catch (e: CancellationException) {
            logger.info("HentFSPforPerson: oppslag avbrutt, oppslagId={}", lookupId)
            throw e
        } catch (e: Exception) {
            logger.error("HentFSPforPerson: oppslag feilet, oppslagId={}, feiltype={}", lookupId, e.javaClass.simpleName)
            sikkerLogger.error("HentFSPforPerson: oppslag feilet, oppslagId=$lookupId, fnr=$fnr", e)
            throw e
        } finally {
            pending.remove(fnr, response)
        }
    }

    fun receive(message: String) {
        val json =
            try {
                Json.parseToJsonElement(message) as? JsonObject
                    ?: throw SerializationException("PRI-melding er ikke et JSON-objekt")
            } catch (e: SerializationException) {
                logger.warn("HentFSPforPerson: Ugyldig JSON-melding mottatt på PRI-topic")
                sikkerLogger.warn("HentFSPforPerson: Ugyldig JSON-melding mottatt på PRI-topic", e)
                return
            }

        if ((json[KafkaConfig.PRI_FELT_NAVN_NOTIS] as? JsonPrimitive)?.contentOrNull == KafkaConfig.FORESPOERSEL_FORKASTET) {
            mottaForkasting(json)
            return
        }

        if ((json[KafkaConfig.PRI_FELT_NAVN_NOTIS] as? JsonPrimitive)?.contentOrNull !=
            KafkaConfig.PRI_NOTIS_FORESPOERSEL_LISTE_FOR_PERSON
        ) {
            logger.info("HentFSPforPerson: ignorerer PRI-melding med annen eller manglende notis")
            return
        }

        logger.info("HentFSPforPerson: mottok svarmelding på PRI-topic")
        val fnr = (json[KafkaConfig.FNR] as? JsonPrimitive)?.contentOrNull
        sikkerLogger.info(
            "HentFSPforPerson: mottok svar, fnr={}, forespoersel_liste={}",
            fnr,
            json[KafkaConfig.PRI_FELT_NAVN_FORESPOERSEL_LISTE],
        )
        if (fnr.isNullOrBlank()) {
            logger.warn("HentFSPforPerson: svar mangler fnr")
            return
        }

        val response = pending[fnr]
        if (response == null) {
            logger.warn("HentFSPforPerson: svar matcher ikke et ventende oppslag")
            sikkerLogger.warn("HentFSPforPerson: svar matcher ikke et ventende oppslag, fnr={}", fnr)
            return
        }
        logger.info("HentFSPforPerson: svar matcher et ventende oppslag")
        val forespoersler =
            try {
                (json[KafkaConfig.PRI_FELT_NAVN_FORESPOERSEL_LISTE] as? JsonArray)
                    ?.map { forespoersel ->
                        val forespoerselObject =
                            forespoersel as? JsonObject
                                ?: throw SerializationException("Ugyldig forespoersel")
                        UUID.fromString(
                            (forespoerselObject[KafkaConfig.PRI_FELT_NAVN_FORESPOERSEL_ID] as? JsonPrimitive)
                                ?.contentOrNull
                                ?: throw SerializationException("Mangler forespoerselId"),
                        )
                        forespoerselObject
                    }
                    ?: throw SerializationException("Mangler forespoersel_liste")
            } catch (e: SerializationException) {
                response.completeExceptionally(e)
                logger.warn("HentFSPforPerson: svar har ugyldig format")
                sikkerLogger.warn("HentFSPforPerson: svar har ugyldig format", e)
                return
            } catch (e: IllegalArgumentException) {
                response.completeExceptionally(e)
                logger.warn("HentFSPforPerson: svar inneholder en ugyldig forespoerselId")
                sikkerLogger.warn("HentFSPforPerson: svar inneholder en ugyldig forespoerselId", e)
                return
            }

        if (response.complete(forespoersler)) {
            logger.info("HentFSPforPerson: svar levert til ventende oppslag, antall={}", forespoersler.size)
        } else {
            logger.warn("HentFSPforPerson: svar kunne ikke leveres, oppslaget er allerede avsluttet")
        }
    }

    private fun mottaForkasting(json: JsonObject) {
        val forespoerselId =
            try {
                UUID.fromString(
                    (json[KafkaConfig.PRI_FELT_NAVN_FORESPOERSEL_ID] as? JsonPrimitive)?.contentOrNull
                        ?: throw IllegalArgumentException("Mangler forespoerselId"),
                )
            } catch (e: IllegalArgumentException) {
                logger.warn("Bekreftelse på forkasting mangler gyldig forespoerselId")
                sikkerLogger.warn("Bekreftelse på forkasting mangler gyldig forespoerselId", e)
                return
            }
        val response = ventendeForkastinger[forespoerselId]
        if (response == null) {
            logger.warn("Bekreftelse matcher ikke en ventende forkasting, forespoerselId={}", forespoerselId)
        } else if (!response.complete(Unit)) {
            logger.warn("Forkasting er allerede bekreftet, forespoerselId={}", forespoerselId)
        }
    }

    private companion object {
        const val RESPONSE_TIMEOUT_MILLIS = 15_000L
    }
}

class PriResponseTimeoutException : RuntimeException("Tidsavbrudd mens vi ventet på svar fra PRI-topic")

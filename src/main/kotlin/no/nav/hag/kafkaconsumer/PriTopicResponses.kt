package no.nav.hag.kafkaconsumer

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import no.nav.hag.kafkaproducer.KafkaConfig
import no.nav.helsearbeidsgiver.utils.log.logger
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

class PriTopicResponses {
    private val logger = this::class.logger()
    private val pending = ConcurrentHashMap<UUID, CompletableDeferred<List<UUID>>>()

    suspend fun request(
        correlationId: UUID,
        sendRequest: () -> Unit,
    ): List<UUID> {
        val response = CompletableDeferred<List<UUID>>()
        check(pending.putIfAbsent(correlationId, response) == null) {
            "Det finnes allerede en PRI-forespørsel med samme korrelasjons-ID."
        }

        try {
            sendRequest()
            return withTimeout(RESPONSE_TIMEOUT_MILLIS.milliseconds) { response.await() }
        } catch (e: TimeoutCancellationException) {
            throw PriResponseTimeoutException()
        } finally {
            pending.remove(correlationId, response)
        }
    }

    fun receive(message: String) {
        val json =
            try {
                Json.parseToJsonElement(message) as? JsonObject
                    ?: throw SerializationException("PRI-melding er ikke et JSON-objekt")
            } catch (e: SerializationException) {
                logger.warn("Ugyldig JSON-melding mottatt på PRI-topic")
                return
            }

        if ((json[KafkaConfig.PRI_FELT_NAVN_NOTIS] as? JsonPrimitive)?.contentOrNull !=
            KafkaConfig.PRI_NOTIS_FORESPOERSEL_LISTE_FOR_PERSON
        ) {
            return
        }

        val correlationId =
            try {
                UUID.fromString(
                    (json[KafkaConfig.PRI_FELT_NAVN_FORESPOERSEL_SVAR_ID] as? JsonPrimitive)
                        ?.contentOrNull
                        ?: throw IllegalArgumentException("Mangler korrelasjons-ID"),
                )
            } catch (e: IllegalArgumentException) {
                logger.warn("PRI-svar mangler en gyldig korrelasjons-ID")
                return
            }

        val response = pending[correlationId] ?: return
        val forespoerselIder =
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
                    }
                    ?: throw SerializationException("Mangler forespoersel_liste")
            } catch (e: SerializationException) {
                response.completeExceptionally(e)
                logger.warn("PRI-svar har ugyldig format")
                return
            } catch (e: IllegalArgumentException) {
                response.completeExceptionally(e)
                logger.warn("PRI-svar inneholder en ugyldig forespoerselId")
                return
            }

        response.complete(forespoerselIder)
    }

    private companion object {
        const val RESPONSE_TIMEOUT_MILLIS = 15_000L
    }
}

class PriResponseTimeoutException : RuntimeException("Tidsavbrudd mens vi ventet på svar fra PRI-topic")

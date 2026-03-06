package no.nav.hag

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import no.nav.hag.kafkaproducer.KafkaConfig.FORESPOERSEL_MANUELT_FORKASTET
import no.nav.hag.kafkaproducer.KafkaConfig.PRI_FELT_NAVN_FORESPOERSEL_ID
import no.nav.hag.kafkaproducer.KafkaConfig.PRI_FELT_NAVN_NOTIS
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.json.toJsonStr
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.util.UUID

interface ForespoerselService {
    suspend fun forkastForespoersel(
        foresporselId: UUID,
        brukernavn: String,
    )
}

class ForespoerselServiceImpl(
    kafkaProducer: KafkaProducer<String, String>,
) : ForespoerselService {
    private val logger = this::class.logger()
    private val sikkerLogger = sikkerLogger()
    private val kafkaProducer = kafkaProducer
    private val topic = "helsearbeidsgiver.pri"

    override suspend fun forkastForespoersel(
        foresporselId: UUID,
        brukernavn: String,
    ) {
        logger.info("Forkaster forespørsel: $foresporselId. Utført av $brukernavn")
        sikkerLogger.info("Forkaster forespørsel: $foresporselId. Utført av $brukernavn")
        val kafkaMessage =
            mapOf(
                PRI_FELT_NAVN_NOTIS to FORESPOERSEL_MANUELT_FORKASTET.toJson(),
                PRI_FELT_NAVN_FORESPOERSEL_ID to foresporselId.toJson(),
            )
        runCatching {
            kafkaProducer.send(
                ProducerRecord(
                    topic,
                    foresporselId.toString(),
                    kafkaMessage.toJsonStr(
                        MapSerializer(String.serializer(), JsonElement.serializer()),
                    ),
                ),
            )
        }.onFailure { error ->
            sikkerLogger.error("Feilet under sending til kafka", error)
            logger.error("Feilet under sending til kafka")
            throw error
        }
    }
}

class MockForespoerselService : ForespoerselService {
    val logger = LoggerFactory.getLogger(FakeServiceImpl::class.java)

    override suspend fun forkastForespoersel(
        foresporselId: UUID,
        brukernavn: String,
    ) {
        logger.info("Forkaster forespørsel: $foresporselId. Utført av $brukernavn")
    }
}

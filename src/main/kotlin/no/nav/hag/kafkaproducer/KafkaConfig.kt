package no.nav.hag.kafkaproducer

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer

object KafkaConfig {
    private val producer by lazy {
        KafkaProducer<String, String>(
            producerProperties(),
            StringSerializer(),
            StringSerializer(),
        )
    }

    fun getKafkaProducer(): KafkaProducer<String, String> = producer

    fun createConsumer(): KafkaConsumer<String, String> =
        KafkaConsumer(
            consumerProperties(),
            StringDeserializer(),
            StringDeserializer(),
        )

    const val PRI_TOPIC = "helsearbeidsgiver.pri"
    private const val PRI_CONSUMER_GROUP_ID = "hag-admin-pri-consumer"

    const val PRI_FELT_NAVN_NOTIS = "notis"
    const val PRI_FELT_NAVN_BEHOV = "@behov"
    const val PRI_FELT_NAVN_FORESPOERSEL_LISTE = "forespoersel_liste"
    const val PRI_FELT_NAVN_FORESPOERSEL_ID = "forespoerselId"
    const val PRI_FELT_NAVN_VEDTAKSPERIODE_ID = "vedtaksperiode_id"
    const val FORESPOERSEL_MANUELT_FORKASTET = "FORESPOERSEL_MANUELT_FORKASTET"
    const val HENT_FORESPOERSLER_FOR_VEDTAKSPERIODE_ID = "HENT_FORESPOERSLER_FOR_VEDTAKSPERIODE_ID"
    const val HENT_FORESPOERSLER_FOR_PERSON = "HENT_FORESPOERSLER_FOR_PERSON"
    const val PRI_NOTIS_FORESPOERSEL_LISTE_FOR_PERSON = "FORESPOERSEL_LISTE_FOR_PERSON"
    const val FNR = "fnr"

    internal fun producerProperties(environment: Map<String, String> = System.getenv()): Map<String, Any> =
        commonProperties(environment) +
            mapOf(
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
                ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION to 1,
                ProducerConfig.MAX_BLOCK_MS_CONFIG to 15_000L,
                ProducerConfig.RETRIES_CONFIG to 2,
                ProducerConfig.ACKS_CONFIG to "all",
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.name,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.name,
            )

    internal fun consumerProperties(environment: Map<String, String> = System.getenv()): Map<String, Any> =
        commonProperties(environment) +
            mapOf(
                ConsumerConfig.GROUP_ID_CONFIG to PRI_CONSUMER_GROUP_ID,
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "latest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java.name,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java.name,
            )

    private fun commonProperties(environment: Map<String, String>): Map<String, Any> {
        fun required(name: String): String =
            requireNotNull(environment[name]?.takeIf(String::isNotBlank)) {
                "Mangler miljøvariabel: $name"
            }

        val password = required("KAFKA_CREDSTORE_PASSWORD")
        return mapOf(
            CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to required("KAFKA_BROKERS"),
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to SecurityProtocol.SSL.name,
            SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "",
            SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to "jks",
            SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to "PKCS12",
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to required("KAFKA_TRUSTSTORE_PATH"),
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to password,
            SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to required("KAFKA_KEYSTORE_PATH"),
            SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to password,
            SslConfigs.SSL_KEY_PASSWORD_CONFIG to password,
        )
    }
}

package no.nav.hag.kafkaproducer

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.serialization.StringSerializer

object KafkaConfig {
    private val kafkaProducer: KafkaProducer<String, String>

    init {
        kafkaProducer = createProducer()
    }

    fun getKafkaProducer(): KafkaProducer<String, String> = kafkaProducer

    private fun createProducer(): KafkaProducer<String, String> =
        KafkaProducer(
            kafkaProperties(),
            StringSerializer(),
            StringSerializer(),
        )

    private fun kafkaProperties() =
        mutableMapOf<String, Any>().apply {
            val pkcs12 = "PKCS12"
            val javaKeystore = "jks"

            put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
            put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
            put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "15000")
            put(ProducerConfig.RETRIES_CONFIG, "2")
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")

            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getenv("KAFKA_BROKERS"))
            put(ProducerConfig.ACKS_CONFIG, "all")
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name)
            put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "")
            put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, javaKeystore)
            put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, pkcs12)
            put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, System.getenv("KAFKA_TRUSTSTORE_PATH"))
            put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, System.getenv("KAFKA_CREDSTORE_PASSWORD"))
            put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, System.getenv("KAFKA_KEYSTORE_PATH"))
            put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, System.getenv("KAFKA_CREDSTORE_PASSWORD"))
            put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, System.getenv("KAFKA_CREDSTORE_PASSWORD"))
        }
}

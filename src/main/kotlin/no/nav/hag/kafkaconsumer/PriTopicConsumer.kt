package no.nav.hag.kafkaconsumer

import no.nav.helsearbeidsgiver.utils.log.logger
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.errors.WakeupException
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

class PriTopicConsumer(
    private val priTopicResponses: PriTopicResponses,
) {
    private val logger = this::class.logger()
    private val running = AtomicBoolean(false)
    private val consumer = createConsumer()
    private val worker =
        Thread(::consume, "pri-topic-consumer").apply {
            isDaemon = true
        }

    fun start() {
        if (running.compareAndSet(false, true)) {
            worker.start()
        }
    }

    fun stop() {
        if (running.getAndSet(false)) {
            consumer.wakeup()
            worker.join()
        }
    }

    private fun consume() {
        try {
            consumer.subscribe(listOf(TOPIC))
            while (running.get()) {
                val records = consumer.poll(Duration.ofSeconds(1))
                if (!records.isEmpty) {
                    records.forEach { record ->
                        priTopicResponses.receive(record.value())
                    }
                    consumer.commitSync()
                }
            }
        } catch (e: WakeupException) {
            if (running.get()) throw e
        } finally {
            consumer.close()
        }
    }

    private fun createConsumer(): KafkaConsumer<String, String> =
        KafkaConsumer(
            mapOf(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to System.getenv("KAFKA_BROKERS"),
                ConsumerConfig.GROUP_ID_CONFIG to GROUP_ID,
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "latest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java.name,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java.name,
                CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to SecurityProtocol.SSL.name,
                SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "",
                SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to "jks",
                SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to "PKCS12",
                SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to System.getenv("KAFKA_TRUSTSTORE_PATH"),
                SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to System.getenv("KAFKA_CREDSTORE_PASSWORD"),
                SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to System.getenv("KAFKA_KEYSTORE_PATH"),
                SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to System.getenv("KAFKA_CREDSTORE_PASSWORD"),
                SslConfigs.SSL_KEY_PASSWORD_CONFIG to System.getenv("KAFKA_CREDSTORE_PASSWORD"),
            ),
            StringDeserializer(),
            StringDeserializer(),
        )

    private companion object {
        const val TOPIC = "helsearbeidsgiver.pri"
        const val GROUP_ID = "hag-admin-pri-consumer"
    }
}

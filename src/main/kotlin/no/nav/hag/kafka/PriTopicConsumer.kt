package no.nav.hag.kafka

import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.WakeupException
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

class PriTopicConsumer(
    private val priTopicResponses: PriTopicResponses,
) {
    private val logger = this::class.logger()
    private val sikkerLogger = sikkerLogger()
    private val running = AtomicBoolean(false)
    private val consumer = KafkaConfig.createConsumer()
    private val worker =
        Thread(::consume, "pri-topic-consumer").apply {
            isDaemon = true
        }

    fun start() {
        if (running.compareAndSet(false, true)) {
            logger.info("Starter PRI-consumer")
            worker.start()
        }
    }

    fun stop() {
        if (running.getAndSet(false)) {
            logger.info("Stopper PRI-consumer")
            consumer.wakeup()
            worker.join()
        }
    }

    private fun consume() {
        try {
            consumer.subscribe(
                listOf(KafkaConfig.PRI_TOPIC),
                object : ConsumerRebalanceListener {
                    override fun onPartitionsAssigned(partitions: Collection<TopicPartition>) {
                        logger.info("PRI-consumer fikk tildelt partisjoner: {}", partitions)
                    }

                    override fun onPartitionsRevoked(partitions: Collection<TopicPartition>) {
                        logger.info("PRI-consumer mistet partisjoner: {}", partitions)
                    }
                },
            )
            logger.info("PRI-consumer abonnerer på {}", KafkaConfig.PRI_TOPIC)
            while (running.get()) {
                val records = consumer.poll(Duration.ofSeconds(1))
                if (!records.isEmpty) {
                    logger.info("PRI-consumer mottok {} meldinger", records.count())
                    records.forEach { record ->
                        priTopicResponses.receive(record.value())
                    }
                    consumer.commitSync()
                    logger.info("PRI-consumer lagret offsets")
                }
            }
        } catch (e: WakeupException) {
            if (running.get()) {
                logger.error("PRI-consumer ble vekket uventet og avsluttes")
                sikkerLogger.error("PRI-consumer ble vekket uventet og avsluttes", e)
                throw e
            }
        } catch (e: Exception) {
            logger.error("PRI-consumer avsluttes etter feil av type {}", e.javaClass.simpleName)
            sikkerLogger.error("PRI-consumer avsluttes etter feil", e)
            throw e
        } finally {
            logger.info("PRI-consumer avslutter og lukker Kafka-consumer")
            try {
                consumer.close()
                logger.info("PRI-consumer er lukket")
            } catch (e: Exception) {
                logger.error("Kunne ikke lukke PRI-consumer, feiltype={}", e.javaClass.simpleName)
                sikkerLogger.error("Kunne ikke lukke PRI-consumer", e)
                throw e
            }
        }
    }
}

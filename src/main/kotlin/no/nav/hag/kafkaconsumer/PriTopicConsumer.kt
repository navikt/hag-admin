package no.nav.hag.kafkaconsumer

import no.nav.hag.kafkaproducer.KafkaConfig
import org.apache.kafka.common.errors.WakeupException
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

class PriTopicConsumer(
    private val priTopicResponses: PriTopicResponses,
) {
    private val running = AtomicBoolean(false)
    private val consumer = KafkaConfig.createConsumer()
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
            consumer.subscribe(listOf(KafkaConfig.PRI_TOPIC))
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
}

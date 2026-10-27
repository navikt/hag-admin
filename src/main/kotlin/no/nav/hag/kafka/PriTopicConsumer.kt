package no.nav.hag.kafka

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.errors.WakeupException
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class PriTopicConsumer(
    private val priTopicResponses: PriTopicResponses,
    private val consumer: Consumer<String, String> = KafkaConfig.createConsumer(),
) {
    private val running = AtomicBoolean(false)
    private val initialized = CompletableFuture<Unit>()
    private val worker =
        Thread(::consume, "pri-topic-consumer").apply {
            isDaemon = true
        }

    fun start() {
        if (running.compareAndSet(false, true)) {
            worker.start()
        }
        try {
            initialized.get(60, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            stop()
            Thread.currentThread().interrupt()
            throw IllegalStateException("Avbrutt under oppstart av PRI-consumer", e)
        } catch (e: java.util.concurrent.ExecutionException) {
            stop()
            throw IllegalStateException("Kunne ikke starte PRI-consumer", e.cause)
        } catch (e: java.util.concurrent.TimeoutException) {
            stop()
            throw IllegalStateException("Tidsavbrudd under oppstart av PRI-consumer", e)
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
                if (!initialized.isDone) {
                    val assignment = consumer.assignment()
                    if (assignment.isNotEmpty()) {
                        assignment.forEach { consumer.position(it) }
                        initialized.complete(Unit)
                    }
                }
                if (!records.isEmpty) {
                    records.forEach { record ->
                        priTopicResponses.receive(record.value())
                    }
                    consumer.commitSync()
                }
            }
        } catch (e: WakeupException) {
            if (running.get()) throw e
        } catch (e: Exception) {
            initialized.completeExceptionally(e)
            throw e
        } finally {
            initialized.completeExceptionally(IllegalStateException("PRI-consumer stoppet før initialisering"))
            consumer.close()
        }
    }
}

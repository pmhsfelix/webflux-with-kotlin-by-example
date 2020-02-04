package org.pedrofelix.examples.reactive

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

private val log = LoggerFactory.getLogger(ReactorTests::class.java)

class ReactorTests {

    @Test
    fun `a play in multiple acts`() {
        val done = CountDownLatch(2)

        log.info("Creating the mono, i.e., the processing pipeline")
        val mono: Mono<Int> = Mono.create<Int> {
            log.info("Mono.create callback called, emitting value")
            it.success(42)
        }
            .map {
                log.info("map: multiplying value by 2")
                it * 2
            }
            .delayElement(Duration.ofMillis(1000))
            .map {
                log.info("map: dividing value by 2")
                it / 2
            }

        log.info("Mono created, resting for a bit...")
        Thread.sleep(1000)
        log.info("Now going to subscribe to the mono")

        log.info("subscribing to mono on main thread")
        mono.subscribe {
            log.info("mono.subscribe called with {}", it)
            done.countDown()
        }
        log.info("subscription done")

        thread(name = "my-thread") {
            log.info("subscribing to mono on a different thread")
            mono.subscribe {
                log.info("mono.subscribe called with {}", it)
                done.countDown()
            }
            log.info("subscription done")
        }

        // waiting for subscribers to receive value before ending the test
        done.await()
    }
}
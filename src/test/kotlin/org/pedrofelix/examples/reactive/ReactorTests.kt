package org.pedrofelix.examples.reactive

import org.junit.jupiter.api.Test
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

private val log = LoggerFactory.getLogger(ReactorTests::class.java)

class ReactorTests {

    @Test
    fun `intro to reactive streams`() {

        val thePublisher: Publisher<Int> = Flux.fromArray(arrayOf(1, 2, 3))
        val theSubscriber: Subscriber<Int> = object : Subscriber<Int> {

            override fun onSubscribe(subscription: Subscription) {
                log.info("onSubscribe called, asking for 10 elements")
                subscription.request(10)
            }

            override fun onNext(t: Int) {
                log.info("onNext called")
            }

            override fun onError(t: Throwable) {
                log.info("onError called")
            }

            override fun onComplete() {
                log.info("onCompleted")
            }
        }

        log.info("about to subscribe")
        thePublisher.subscribe(theSubscriber)
        log.info("subscribe done")
    }

    @Test
    fun `composition is the real deal`() {
        val publisher0: Flux<Int> = Flux.fromArray(arrayOf(1, 2, 3))
        val publisher1: Flux<String> = publisher0.map { it.toString() }
        val publisher2: Mono<String> = publisher1.reduce { acc, elem -> "$acc, $elem" }

        publisher2.subscribe { log.info("received $it") }
    }

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
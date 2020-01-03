package org.pedrofelix.examples.reactive.webhandler

import org.pedrofelix.examples.reactive.utils.use
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.HttpHandler
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter
import org.springframework.web.server.*
import org.springframework.web.server.adapter.WebHttpHandlerBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.netty.http.server.HttpServer
import java.nio.charset.StandardCharsets

private val log = LoggerFactory.getLogger(HelloWebHandler::class.java)

/**
 * A [WebHandler] differs from an [HttpHandler] in the following ways:
 * - It is called at the end of a pipeline comprised of [WebFilter] and [WebExceptionHandler] instances.
 * - Has access to a richer context ([ServerWebExchange]), including
 *  - form data
 *  - request principal
 *  - exchange attributes set by filters
 *
 *  So, a [WebHandler] seems more equivalent to a servlet than a `HttpHandler`
 */
class HelloWebHandler : WebHandler {

    private val msg = "Hello World, from an WebHandler. Try 'exception/synchronous' and 'exception/asynchronous'"
    private val msgBytes = msg.toByteArray()
    private val msgSize = msgBytes.size

    override fun handle(exchange: ServerWebExchange): Mono<Void> {
        // Retrieving information from the request message
        log.info("Request: method={}, URI={}, Accept={}",
                exchange.request.method, exchange.request.uri, exchange.request.headers.accept)

        // Setting information into the response message
        exchange.response.statusCode = HttpStatus.OK
        exchange.response.headers.add("Content-Type", "text/plain")
        exchange.response.headers.add("Content-Length", msgSize.toString())

        // Simulate an exception
        if(exchange.request.uri.path.endsWith("exception/synchronous")) {
            // throws a synchronous exception
            throw Exception("Synchronous exception")
        }
        if(exchange.request.uri.path.endsWith("exception/asynchronous")) {
            // throws a asynchronous exception
            return Mono.error(Exception("Asynchronous exception"))
        }

        return exchange.response.writeWith(
                Flux.just(exchange.response.bufferFactory().wrap(msgBytes)))
    }
}

class ExampleWebFilter : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        log.info("Filter call on the inbound journey")
        // forwarding request to the next handler
        return chain.filter(exchange)
                .doOnTerminate {
                    log.info("Filter called on the outbound journey: isCommited={}", exchange.response.isCommitted)
                }
    }
}

class ExampleExceptionHandler : WebExceptionHandler {

    override fun handle(exchange: ServerWebExchange, ex: Throwable): Mono<Void> {
        log.warn("Handling exception: {}", ex.message)
        val msg = "An exception occurred: ${ex.message}"
        val msgBytes = msg.toByteArray(StandardCharsets.UTF_8)
        return exchange.response.run {
            statusCode = HttpStatus.INTERNAL_SERVER_ERROR
            headers.set("Content-Type", "text/plain")
            headers.set("Content-Length", msgBytes.size.toString())
            headers.set("My-Header", "My-Value")

            // Controlling the response body can be done by providing a `Publisher<DataBuffer>`
            // This allows the body to be written in an asynchronous and incremental fashion.
            writeWith(
                    Flux.just(bufferFactory().wrap(msgBytes)))
        }
    }
}

fun main() {

    /**
     * An [WebHttpHandlerBuilder] is used to create the pipeline
     * - At the end it uses a `WebHandler`.
     * - At the beginning is implements the `HttpHandler` interface
     * - Can contain multiple _filters_ and _exception handlers_
     */
    val httpHandler: HttpHandler = WebHttpHandlerBuilder.webHandler(HelloWebHandler())
            .filter(ExampleWebFilter())
            .exceptionHandler(ExampleExceptionHandler())
            .build()

    // Hosting the resulting [HttpHandler] is "business as usual"
    val adapter = ReactorHttpHandlerAdapter(httpHandler)
    HttpServer.create()
            .host("localhost")
            .port(8080)
            .handle(adapter)
            .bind()
            .block().use { server ->
                log.info("server listening at {}", server?.address())
                readLine()
            }
    log.info("server closed")
}
package org.pedrofelix.examples.reactive.httphandler

import org.pedrofelix.examples.reactive.utils.use
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.HttpHandler
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.netty.http.server.HttpServer

/**
 * [HttpHandler] seems to be the lowest common abstraction around the handling
 * of an HTTP exchange, playing a role similar to servlets in the old Spring MVC.
 */

private val log = LoggerFactory.getLogger(HelloHttpHandler::class.java)

class HelloHttpHandler : HttpHandler {

    private val msg = "Hello World, from an HttpHandler"
    private val msgBytes = msg.toByteArray()
    private val msgSize = msgBytes.size

    /**
     * Contains a single [handle] method, receiving both a [ServerHttpRequest] and a [ServerHttpResponse]
     * - [ServerHttpRequest] - represents a "server-side reactive HTTP request message"
     * - [ServerHttpResponse] - represents a "server-side reactive HTTP response message"
     * The method returns a [Mono<Void>] that is used to signal the end of the handling.
     * Note that since this method is asynchronous, it will return *before* the handling is completed.
     */
    override fun handle(request: ServerHttpRequest, response: ServerHttpResponse): Mono<Void> {

        // Retrieving information from the request message
        log.info("Request: method={}, URI={}, Accept={}",
                request.method, request.uri, request.headers.accept)

        // Setting information into the response message
        return response.run {
            statusCode = HttpStatus.OK
            headers.add("Content-Type", "text/plain")
            headers.add("Content-Length", msgSize.toString())

            // Controlling the response body can be done by providing a `Publisher<DataBuffer>`
            // This allows the body to be written in an asynchronous and incremental fashion.
            writeWith(
                    Flux.just(bufferFactory().wrap(msgBytes)))
        }
    }
}

fun main() {
    // The `ReactorHttpHandlerAdapter` does the adaptation between the `HttpHandler` model
    // and the underlying Netty `HttpServer` model.
    val adapter = ReactorHttpHandlerAdapter(HelloHttpHandler())
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

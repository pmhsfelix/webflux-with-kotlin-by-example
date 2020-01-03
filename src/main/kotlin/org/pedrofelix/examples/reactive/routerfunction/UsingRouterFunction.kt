package org.pedrofelix.examples.reactive.routerfunction

import kotlinx.coroutines.delay
import org.pedrofelix.examples.reactive.utils.use
import org.slf4j.LoggerFactory
import org.springframework.http.server.reactive.HttpHandler
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter
import org.springframework.web.reactive.function.server.*
import reactor.netty.http.server.HttpServer

private val log = LoggerFactory.getLogger("using-router-functions")

fun main() {

    /**
     * The [router] function allows the declarative and nested definition of handling functions
     * The outer result is a [RouterFunction].
     *
     * Handling functions are functions from [ServerRequest] to [Mono<ServerResponse>]: `(ServerRequest) -> Mono<out ServerResponse>`.
     *
     * A [RouterFunction] is an interface with a `Mono<HandlerFunction<T>> route(ServerRequest request)`,
     * i.e, it is something that given a request, asynchronously returns an handler function.
     */
    val routerFunction: RouterFunction<ServerResponse> = router {
        path("/examples").nest {
            GET("/") { req ->
                log.info("Request received: method={}, URI={}", req.method(), req.uri())
                ok().bodyValue("Hello world, from /examples")
            }
            GET("/{id}") { req ->
                val id = req.pathVariable("id")
                log.info("Request received: method={}, URI={}, id={}",
                        req.method(), req.uri(), id)
                ok().bodyValue("Hello world, from /examples/{id}, with id=$id")
            }
        }
    }

    /**
     * The [coRouter] function allows the use of `suspend` functions as handlers
     * The outer result is still a `RouterFunction`.
     */
    val coRouterFunction: RouterFunction<ServerResponse> = coRouter {
        path("/suspend").nest {
            GET("/") { req -> // this is a suspend lambda (note the delay below)
                log.info("Request received: method={}, URI={}", req.method(), req.uri())
                delay(1000) // this is a suspension point
                ok().bodyValueAndAwait("Hello world, from /suspend")
            }
            GET("/{id}") { req -> // this is a suspend lambda (note the delay below)
                val id = req.pathVariable("id")
                log.info("Request received: method={}, URI={}, id={}",
                        req.method(), req.uri(), id)
                delay(1000) // this is a suspension point
                ok().bodyValueAndAwait("Hello world, from /suspend/{id}, with id=$id")
            }

            // The DSL also allows the definition of suspend filters that intercept the handling pipeline
            filter { req, next ->
                log.info("Request received: method={}, URI={}", req.method(), req.uri())
                delay(1000) // suspension point
                val resp = next(req) // suspension point
                log.info("Response produced: status={}", resp.statusCode())
                resp
            }
        }
    }

    // Example of using the filter logic encapsulated in a function
    fun CoRouterFunctionDsl.addLog() = filter { req, next ->
        log.info("inbound: method={}, URI={}", req.method(), req.uri())
        val resp = next(req)
        log.info("outbound: status={}", resp.statusCode())
        resp
    }

    val coRouterFunction2: RouterFunction<ServerResponse> = coRouter {
        path("/suspend2").nest {
            GET("/") { req -> // this is a suspend lambda (note the delay below)
                log.info("Request received: method={}, URI={}", req.method(), req.uri())
                delay(1000) // this is a suspension point
                ok().bodyValueAndAwait("Hello world, from /suspend")
            }
            GET("/{id}") { req -> // this is a suspend lambda (note the delay below)
                val id = req.pathVariable("id")
                log.info("Request received: method={}, URI={}, id={}",
                        req.method(), req.uri(), id)
                delay(1000) // this is a suspension point
                ok().bodyValueAndAwait("Hello world, from /suspend/{id}, with id=$id")
            }

            // adding log (via adding a filter)
            addLog()
        }
    }

    // It is possible to compose router functions
    val composedRouterFunction = routerFunction
            .andOther(coRouterFunction)
            .andOther(coRouterFunction2)

    // Finally, we can create a low-level `HttpHandler` from a `RouterFunction`
    // and then provide it to a server
    val httpHandler: HttpHandler = RouterFunctions.toHttpHandler(composedRouterFunction)

    val adapter = ReactorHttpHandlerAdapter(httpHandler)
    HttpServer.create()
            .host("localhost")
            .port(8080)
            .handle(adapter)
            .bind()
            .block()
            .use { server ->
                log.info("server listening at {}", server?.address())
                readLine()
            }
    log.info("server closed")
}
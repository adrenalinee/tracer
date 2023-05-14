package malibu.tracer.webflux

import malibu.tracer.EachRequestContext
import malibu.tracer.TracerContext
import org.springframework.core.annotation.Order
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebExceptionHandler
import reactor.core.publisher.Mono

@Order(-2)
class TracerWebExceptionHandler: WebExceptionHandler {
    override fun handle(exchange: ServerWebExchange, ex: Throwable): Mono<Void> {
//        val request = exchange.request
//        val rawQuery = request.uri.rawQuery
//        val query = if (StringUtils.hasText(rawQuery)) "?$rawQuery" else ""
//        val httpMethod = request.method
//        val description = "HTTP " + httpMethod + " \"" + request.path + query + "\""
//        return Mono.error<Void>(ex).checkpoint("$description [ExceptionHandlingWebHandler]")

//        return Mono.subscriberContext()
//            .flatMap { context ->
//                context.getOrDefault<EachRequestContext?>(TracerContext.ATTR_REQUEST_CONTEXT, null)
//                    ?.also { requestContext -> requestContext.setError(ex) }
//
//                Mono.error<Void>(ex)
//            }



        exchange.attributes[TracerContext.ATTR_REQUEST_ERROR] = ex

        return Mono.error(ex)
    }
}
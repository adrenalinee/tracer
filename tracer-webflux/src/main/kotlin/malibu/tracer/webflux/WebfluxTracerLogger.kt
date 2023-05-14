package malibu.tracer.webflux

import malibu.tracer.EachRequestContext
import malibu.tracer.TraceSpanId
import malibu.tracer.TracerContext
import malibu.tracer.TracerLogger
import malibu.tracer.io.TraceLog
import reactor.core.publisher.Mono

class WebfluxTracerLogger(
    private val tracerLogger: TracerLogger
) {

    fun isDebugEnabled(): Boolean {
        return tracerLogger.isDebugEnabled()
    }

    fun isInfoEnabled(): Boolean {
        return tracerLogger.isInforEnabled()
    }

    fun debug(traceLog: TraceLog, message: String): Mono<Void> {
        return getTraceSpanId()
            .map { traceSpanId ->
                tracerLogger.debug(traceLog, message, null, traceSpanId)
            }.then()
    }

    fun info(traceLog: TraceLog, message: String): Mono<Void> {
        return getTraceSpanId()
            .map { traceSpanId ->
                tracerLogger.infor(traceLog, message, null, traceSpanId)
            }.then()
    }

    fun error(traceLog: TraceLog, error: Throwable?, message: String): Mono<Void> {
        return getTraceSpanId()
            .map { traceSpanId ->
                tracerLogger.error(traceLog, message, error, null, traceSpanId)
            }.then()
    }

    fun getRunId(): String {
        return tracerLogger.getRunId()
    }

    fun getTraceSpanId(): Mono<TraceSpanId> {
        return Mono.deferContextual { contextView ->
            contextView.getOrDefault<malibu.tracer.EachRequestContext?>(TracerContext.ATTR_REQUEST_CONTEXT, null)
                ?.let { it.traceSpanId }
                ?.let { Mono.just(it) }
                ?: Mono.empty()
        }
//        return Mono.subscriberContext()
//            .flatMap { context ->
//                context.getOrDefault<EachRequestContext?>(TracerContext.ATTR_REQUEST_CONTEXT, null)
//                    ?.let { it.traceSpanId }
//                    ?.let { Mono.just(it) }
//                    ?: Mono.empty()
//            }
    }

    fun getRequestContext(): Mono<malibu.tracer.EachRequestContext> {
        return Mono.deferContextual { contextView ->
            contextView.getOrDefault<malibu.tracer.EachRequestContext?>(TracerContext.ATTR_REQUEST_CONTEXT, null)
                ?.let { Mono.just(it) }
                ?: Mono.empty()
        }

//        return Mono.subscriberContext()
//            .flatMap { context ->
//                context.getOrDefault<EachRequestContext?>(TracerContext.ATTR_REQUEST_CONTEXT, null)
//                    ?.let { Mono.just(it) }
//                    ?: Mono.empty<EachRequestContext>()
//            }
    }
}

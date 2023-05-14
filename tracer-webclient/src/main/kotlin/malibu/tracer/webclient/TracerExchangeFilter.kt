package malibu.tracer.webclient

import malibu.tracer.*
import malibu.tracer.io.ConnectHttpLog
import org.springframework.web.reactive.function.client.*
import reactor.core.publisher.Mono
import java.util.*

/**
 * 이 필터를 webClient에 등록하면 trace logging 을 할 수 있다.
 *
 * ex)
 * <code>
 * WebClient.builder()
 *   .filter(tracerExchangeFilter)
 * </code>
 */
class TracerExchangeFilter(
    private val tracerLogger: TracerLogger,
    private val tracerWebClientContext: TracerWebClientContext
): ExchangeFilterFunction {

//    companion object {
//        private val traceDataCreatorRegistry = TraceDataCreatorRegistry.getInstance()
//    }

    /**
     *
     */
    override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> {
        val startedTime = System.currentTimeMillis()
        val connectId = if (tracerLogger.isDebugDetailEnabled()) {
            UUID.randomUUID().toString().substring(0, 10)
        } else {
            null
        }

//        return Mono.subscriberContext()
//            .flatMap { context ->
        return Mono.deferContextual { contextView ->
            val traceSpanId = contextView.getOrDefault<malibu.tracer.EachRequestContext?>(TracerContext.ATTR_REQUEST_CONTEXT, null)
                ?.let { requestContext ->
                    if (requestContext.isInclude.not()) {
                        return@deferContextual next.exchange(request)
                    }

                    requestContext.traceSpanId
                }

            lateinit var responseAndBodyHolder: ClientResponseAndBodyHolder
            val requestAndBodyHolder = ClientRequestAndBodyHolder(request, traceSpanId, tracerWebClientContext.traceRequestBody)
            next.exchange(requestAndBodyHolder.clientRequest)
                .map { response ->
                    responseAndBodyHolder = ClientResponseAndBodyHolder(response, tracerWebClientContext.traceResponseBody)
                    responseAndBodyHolder.onResponseBodyReadComplate = {
                        if (tracerLogger.isInforEnabled()) {
                            val httpLog = createHttpLog(startedTime, requestAndBodyHolder, responseAndBodyHolder)
                            tracerLogger.infor(
                                httpLog,
                                "HTTP ${httpLog.method}: ${httpLog.url} ${httpLog.status}",
                                EachConnectContext(request, response),
                                traceSpanId,
                                connectId
                            )

                            detailLogging(traceSpanId, connectId, httpLog)
                        }
                    }

                    responseAndBodyHolder.clientResponse
                }
//                .doOnSuccess { response ->
//                    if (tracerLogger.isInfoEnabled()) {
//                        tracerLogger.info(
//                            traceSpanId,
//                            createHttpLog(startedTime, requestAndBodyHolder, responseAndBodyHolder),
//                            traceDataCreatorRegistry.traceLogType(DefinedTraceLogType.CONNECT)
//                                .create(DefinedProtocolType.HTTP,  EachConnectContext(request, response)),
//                            "HTTP send: ${request.method()} ${request.url()} ${response.statusCode()}"
//                        )
//                    }
//                }
                .doOnError { error ->
                    val httpLog = createHttpLog(startedTime, requestAndBodyHolder, null, error)
                    tracerLogger.error(
                        httpLog,
                        "HTTP ${request.method()}: ${request.url()}",
                        error,
                        EachConnectContext(request, null, error),
                        traceSpanId,
                    )

                    detailLogging(traceSpanId, connectId, httpLog)
                }
        }
    }

    private fun detailLogging(
        traceSpanId: TraceSpanId?,
        connectId: String?,
        httpLog: ConnectHttpLog
    ) {
        if (tracerLogger.isDebugDetailEnabled()) {
            connectId!!

            val reqBody = if (tracerWebClientContext.traceRequestBody) {
                httpLog.reqBody
            } else {
                "[warn: request body not tracing!!]"
            }
            val resBody = if (tracerWebClientContext.traceResponseBody) {
                httpLog.resBody
            } else {
                "[warn: response body not tracing!!]"
            }
            tracerLogger.deDat(
                httpLog,
                "HTTP request body: $reqBody",
                traceSpanId,
                connectId
            )
            tracerLogger.deDat(
                httpLog,
                "HTTP response body: $resBody",
                traceSpanId,
                connectId
            )
        }
    }

    private fun createHttpLog(
        startedTime: Long,
        requestAndBodyHolder: ClientRequestAndBodyHolder,
        responseAndBodyHolder: ClientResponseAndBodyHolder?,
        throwable: Throwable? = null
    ): ConnectHttpLog {

        return ConnectHttpLog(
            elapsedTime = System.currentTimeMillis() - startedTime,
            method = requestAndBodyHolder.clientRequest.method().name(),
            url = requestAndBodyHolder.clientRequest.url().toString(),
            path = requestAndBodyHolder.clientRequest.url().path,
            reqHeaders = if (tracerWebClientContext.traceRequestHeaders) {
                requestAndBodyHolder.clientRequest.headers()
            } else {
                null
            },
            reqBody = if (tracerWebClientContext.traceRequestBody) {
                requestAndBodyHolder.genRequestBody()
            } else {
                null
            },
            status = responseAndBodyHolder?.clientResponse?.statusCode()?.value(),
            resHeaders = if (tracerWebClientContext.traceResponseHeaders) {
                responseAndBodyHolder?.clientResponse?.headers()?.asHttpHeaders()
            } else {
                null
            },
            resBody = if (tracerWebClientContext.traceResponseBody) {
                responseAndBodyHolder?.genResponseBody()
            } else {
                null
            },
            error = throwable?.traceToString()
        )
    }
}

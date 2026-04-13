package malibu.tracer.webclient

import malibu.tracer.EachRequestContext
import malibu.tracer.TraceSpanId
import malibu.tracer.TracerContext
import malibu.tracer.TracerLogger
import malibu.tracer.traceToString
import malibu.tracer.io.ConnectHttpLog
import malibu.tracer.webmvc.toMultiValueMap
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono
import java.util.UUID

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
) : ExchangeFilterFunction {

    override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> {
        val startedTime = System.currentTimeMillis()

        return Mono.deferContextual { contextView ->
            val traceSpanId = contextView.getOrDefault<EachRequestContext>(TracerContext.ATTR_REQUEST_CONTEXT, null)
                ?.let { requestContext ->
                    if (requestContext.isInclude.not()) {
                        return@deferContextual next.exchange(request)
                    }

                    requestContext.traceSpanId
                }

            val requestLoggingEnabled = tracerWebClientContext.shouldTraceRequest(request)
            val responseLoggingEnabled = tracerWebClientContext.shouldTraceResponse(request)
            if (requestLoggingEnabled.not() && responseLoggingEnabled.not()) {
                return@deferContextual next.exchange(request)
            }

            val connectId = if (tracerLogger.isDebugDetailEnabled()) {
                UUID.randomUUID().toString().substring(0, 10)
            } else {
                null
            }

            lateinit var responseAndBodyHolder: ClientResponseAndBodyHolder
            val requestAndBodyHolder = ClientRequestAndBodyHolder(
                request,
                traceSpanId,
                tracerWebClientContext.traceRequestBody && requestLoggingEnabled,
                tracerWebClientContext.maxPayloadLength
            )

            next.exchange(requestAndBodyHolder.clientRequest)
                .map { response ->
                    responseAndBodyHolder = ClientResponseAndBodyHolder(
                        response,
                        tracerWebClientContext.traceResponseBody && responseLoggingEnabled,
                        tracerWebClientContext.maxPayloadLength
                    )
                    responseAndBodyHolder.onResponseBodyReadComplate = {
                        emitRequestLog(
                            startedTime = startedTime,
                            requestAndBodyHolder = requestAndBodyHolder,
                            responseAndBodyHolder = responseAndBodyHolder,
                            traceSpanId = traceSpanId,
                            connectId = connectId,
                            requestLoggingEnabled = requestLoggingEnabled
                        )
                        emitResponseLog(
                            startedTime = startedTime,
                            request = request,
                            response = response,
                            requestAndBodyHolder = requestAndBodyHolder,
                            responseAndBodyHolder = responseAndBodyHolder,
                            traceSpanId = traceSpanId,
                            connectId = connectId,
                            responseLoggingEnabled = responseLoggingEnabled
                        )
                    }

                    responseAndBodyHolder.clientResponse
                }
                .doOnError { error ->
                    emitRequestLog(
                        startedTime = startedTime,
                        requestAndBodyHolder = requestAndBodyHolder,
                        responseAndBodyHolder = null,
                        traceSpanId = traceSpanId,
                        connectId = connectId,
                        requestLoggingEnabled = requestLoggingEnabled
                    )
                    emitResponseErrorLog(
                        startedTime = startedTime,
                        request = request,
                        requestAndBodyHolder = requestAndBodyHolder,
                        traceSpanId = traceSpanId,
                        connectId = connectId,
                        responseLoggingEnabled = responseLoggingEnabled,
                        error = error
                    )
                }
        }
    }

    private fun emitRequestLog(
        startedTime: Long,
        requestAndBodyHolder: ClientRequestAndBodyHolder,
        responseAndBodyHolder: ClientResponseAndBodyHolder?,
        traceSpanId: TraceSpanId?,
        connectId: String?,
        requestLoggingEnabled: Boolean
    ) {
        if (requestLoggingEnabled.not() || tracerLogger.isInforEnabled().not()) {
            return
        }

        val httpLog = createRequestHttpLog(startedTime, requestAndBodyHolder)
        tracerLogger.infor(
            httpLog,
            "HTTP request: ${httpLog.method} ${httpLog.url}",
            EachConnectContext(requestAndBodyHolder.clientRequest, responseAndBodyHolder?.clientResponse),
            traceSpanId,
            connectId
        )

        emitRequestDetailLog(traceSpanId, connectId, httpLog)
    }

    private fun emitResponseLog(
        startedTime: Long,
        request: ClientRequest,
        response: ClientResponse,
        requestAndBodyHolder: ClientRequestAndBodyHolder,
        responseAndBodyHolder: ClientResponseAndBodyHolder,
        traceSpanId: TraceSpanId?,
        connectId: String?,
        responseLoggingEnabled: Boolean
    ) {
        if (responseLoggingEnabled.not() || tracerLogger.isInforEnabled().not()) {
            return
        }

        val httpLog = createResponseHttpLog(startedTime, requestAndBodyHolder, responseAndBodyHolder)
        tracerLogger.infor(
            httpLog,
            "HTTP response: ${httpLog.method} ${httpLog.url} ${httpLog.status}",
            EachConnectContext(request, response),
            traceSpanId,
            connectId
        )

        emitResponseDetailLog(traceSpanId, connectId, httpLog)
    }

    private fun emitResponseErrorLog(
        startedTime: Long,
        request: ClientRequest,
        requestAndBodyHolder: ClientRequestAndBodyHolder,
        traceSpanId: TraceSpanId?,
        connectId: String?,
        responseLoggingEnabled: Boolean,
        error: Throwable
    ) {
        if (responseLoggingEnabled.not()) {
            return
        }

        val httpLog = createResponseHttpLog(startedTime, requestAndBodyHolder, null, error)
        tracerLogger.error(
            httpLog,
            "HTTP response: ${request.method()} ${request.url()}",
            error,
            EachConnectContext(request, null, error),
            traceSpanId,
            connectId
        )

        emitResponseDetailLog(traceSpanId, connectId, httpLog)
    }

    private fun emitRequestDetailLog(
        traceSpanId: TraceSpanId?,
        connectId: String?,
        httpLog: ConnectHttpLog
    ) {
        if (tracerLogger.isDebugDetailEnabled().not()) {
            return
        }

        connectId!!
        val reqBody = if (tracerWebClientContext.traceRequestBody) {
            httpLog.reqBody
        } else {
            "[warn: request body not tracing!!]"
        }
        tracerLogger.deDat(
            httpLog,
            "HTTP request body: $reqBody",
            traceSpanId,
            connectId
        )
    }

    private fun emitResponseDetailLog(
        traceSpanId: TraceSpanId?,
        connectId: String?,
        httpLog: ConnectHttpLog
    ) {
        if (tracerLogger.isDebugDetailEnabled().not()) {
            return
        }

        connectId!!
        val resBody = if (tracerWebClientContext.traceResponseBody) {
            httpLog.resBody
        } else {
            "[warn: response body not tracing!!]"
        }
        tracerLogger.deDat(
            httpLog,
            "HTTP response body: $resBody",
            traceSpanId,
            connectId
        )
    }

    private fun createRequestHttpLog(
        startedTime: Long,
        requestAndBodyHolder: ClientRequestAndBodyHolder
    ): ConnectHttpLog {
        return ConnectHttpLog(
            elapsedTime = System.currentTimeMillis() - startedTime,
            method = requestAndBodyHolder.clientRequest.method().name(),
            url = requestAndBodyHolder.clientRequest.url().toString(),
            path = requestAndBodyHolder.clientRequest.url().path,
            reqHeaders = if (tracerWebClientContext.traceRequestHeaders) {
                requestAndBodyHolder.clientRequest.headers().toMultiValueMap()
            } else {
                null
            },
            reqBody = if (tracerWebClientContext.traceRequestBody) {
                requestAndBodyHolder.genRequestBody()
            } else {
                null
            }
        )
    }

    private fun createResponseHttpLog(
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
            status = responseAndBodyHolder?.clientResponse?.statusCode()?.value(),
            resHeaders = if (tracerWebClientContext.traceResponseHeaders) {
                responseAndBodyHolder?.clientResponse?.headers()?.asHttpHeaders()?.toMultiValueMap()
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

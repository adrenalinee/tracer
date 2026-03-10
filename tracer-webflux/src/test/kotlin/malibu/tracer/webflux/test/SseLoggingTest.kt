package malibu.tracer.webflux.test

import malibu.tracer.TraceSpanId
import malibu.tracer.TracerLogger
import malibu.tracer.io.ResponseHttpLog
import malibu.tracer.io.TraceLog
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.given
import org.mockito.kotlin.nullableArgumentCaptor
import org.mockito.kotlin.verify
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(
    classes = [LoggerTestConfiguration::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class SseLoggingTest {

    @MockitoBean
    lateinit var tracerLogger: TracerLogger

    @LocalServerPort
    var port: Int = 0

    @Test
    fun sseResponseShouldStreamAndBeLogged() {
        given(tracerLogger.isInforEnabled()).willReturn(true)
        given(tracerLogger.isDebugDetailEnabled()).willReturn(true)

        val result = WebTestClient.bindToServer()
            .baseUrl("http://localhost:$port")
            .build()
            .get()
            .uri("/sse")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus().isOk
            .returnResult(object : org.springframework.core.ParameterizedTypeReference<ServerSentEvent<String>>() {})

        val responseEvents = result.responseBody
            .map { it.data()!! }
            .collectList()
            .block()

        assertThat(responseEvents).containsExactly("first", "second")

        val responseLogCaptor = argumentCaptor<TraceLog>()
        val responseTraceSpanCaptor = nullableArgumentCaptor<TraceSpanId>()
        verify(tracerLogger, atLeastOnce()).infor(
            traceLog = responseLogCaptor.capture(),
            message = any(),
            logContext = any(),
            traceSpanId = responseTraceSpanCaptor.capture(),
            additionalLogId = anyOrNull()
        )

        val responseTraceLog = responseLogCaptor.allValues
            .filterIsInstance<ResponseHttpLog>()
            .last()

        assertThat(responseTraceLog.status).isEqualTo(200)
        assertThat(responseTraceLog.body).contains("data:first")
        assertThat(responseTraceLog.body).contains("data:second")
        assertThat(responseTraceSpanCaptor.lastValue).isNotNull

        val detailLogCaptor = argumentCaptor<String>()
        verify(tracerLogger, atLeast(2)).deDat(
            traceLog = any(),
            message = detailLogCaptor.capture(),
            traceSpanId = anyOrNull(),
            additionalLogId = anyOrNull()
        )

        assertThat(detailLogCaptor.allValues).contains("HTTP response sse event: id:1\nevent:message\ndata:first")
        assertThat(detailLogCaptor.allValues).contains("HTTP response sse event: id:2\nevent:message\ndata:second")
    }
}

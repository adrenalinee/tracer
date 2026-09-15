package malibu.tracer.webflux.test

import malibu.tracer.TraceSpanId
import malibu.tracer.TracerLogger
import malibu.tracer.io.RequestHttpLog
import malibu.tracer.io.ResponseHttpLog
import malibu.tracer.io.TraceLog
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.given
import org.mockito.kotlin.nullableArgumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(
    classes = [LoggerTestConfiguration::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class ErrorResponseBodyLoggingTest {

    @MockitoBean
    lateinit var tracerLogger: TracerLogger

    @LocalServerPort
    var port: Int = 0

    @Test
    fun responseStatusExceptionShouldLogGeneratedErrorBody() {
        doTest("/response-status-exception", HttpStatus.BAD_REQUEST)
    }

    @Test
    fun unhandledErrorShouldLogGeneratedErrorBody() {
        doTest("/unhandled-error", HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun notFoundShouldLogGeneratedErrorBody() {
        doTest("/undefined-path", HttpStatus.NOT_FOUND)
    }

    private fun doTest(path: String, status: HttpStatus) {
        given(tracerLogger.isInforEnabled()).willReturn(true)
        given(tracerLogger.isDebugDetailEnabled()).willReturn(true)

        val responseBody = WebTestClient.bindToServer()
            .baseUrl("http://localhost:$port")
            .build()
            .get()
            .uri(path)
            .exchange()
            .expectStatus().isEqualTo(status)
            .expectBody(String::class.java)
            .returnResult()
            .responseBody

        assertThat(responseBody).isNotBlank()

        val requestLogCaptor = argumentCaptor<TraceLog>()
        val requestTraceSpanCaptor = nullableArgumentCaptor<TraceSpanId>()
        verify(tracerLogger, times(1)).infor(
            traceLog = requestLogCaptor.capture(),
            message = any(),
            logContext = any(),
            traceSpanId = requestTraceSpanCaptor.capture(),
            additionalLogId = anyOrNull()
        )

        val responseLogCaptor = argumentCaptor<TraceLog>()
        val errorCaptor = nullableArgumentCaptor<Throwable>()
        val responseTraceSpanCaptor = nullableArgumentCaptor<TraceSpanId>()
        verify(tracerLogger, times(1)).error(
            traceLog = responseLogCaptor.capture(),
            message = any(),
            error = errorCaptor.capture(),
            logContext = any(),
            traceSpanId = responseTraceSpanCaptor.capture(),
            additionalLogId = anyOrNull()
        )

        assertThat(requestLogCaptor.firstValue).isInstanceOf(RequestHttpLog::class.java)
        assertThat((requestLogCaptor.firstValue as RequestHttpLog).method).isEqualTo(HttpMethod.GET.name())
        assertThat((requestLogCaptor.firstValue as RequestHttpLog).path).isEqualTo(path)
        assertThat(responseLogCaptor.firstValue).isInstanceOf(ResponseHttpLog::class.java)
        assertThat((responseLogCaptor.firstValue as ResponseHttpLog).method).isEqualTo(HttpMethod.GET.name())
        assertThat((responseLogCaptor.firstValue as ResponseHttpLog).path).isEqualTo(path)
        assertThat((responseLogCaptor.firstValue as ResponseHttpLog).status).isEqualTo(status.value())
        assertThat((responseLogCaptor.firstValue as ResponseHttpLog).body).isEqualTo(responseBody)
        assertThat(requestTraceSpanCaptor.firstValue).isNotNull
        assertThat(responseTraceSpanCaptor.firstValue).isNotNull
        assertThat(responseTraceSpanCaptor.firstValue!!.traceId).isEqualTo(requestTraceSpanCaptor.firstValue!!.traceId)

        val detailLogCaptor = argumentCaptor<String>()
        verify(tracerLogger, times(2)).deDat(
            traceLog = any(),
            message = detailLogCaptor.capture(),
            traceSpanId = anyOrNull(),
            additionalLogId = anyOrNull()
        )
        assertThat(detailLogCaptor.allValues).contains("HTTP response body: $responseBody")
    }
}

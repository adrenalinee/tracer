package malibu.tracer.webclient.test

import malibu.tracer.TraceSpanId
import malibu.tracer.TracerLogger
import malibu.tracer.io.ConnectHttpLog
import malibu.tracer.io.TraceLog
import malibu.tracer.webclient.TracerExchangeFilter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.timeout
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.given
import org.mockito.kotlin.never
import org.mockito.kotlin.nullableArgumentCaptor
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.reactive.function.client.WebClient

@SpringBootTest(
    classes = [SelectiveLoggingTestConfiguration::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class SelectiveLoggingTest {

    @MockitoBean
    lateinit var tracerLogger: TracerLogger

    @Autowired
    lateinit var tracerExchangeFilter: TracerExchangeFilter

    @LocalServerPort
    var port: Int = 0

    @BeforeEach
    fun setUp() {
        given(tracerLogger.isInforEnabled()).willReturn(true)
        given(tracerLogger.isDebugDetailEnabled()).willReturn(false)
    }

    @Test
    fun shouldLogOnlyRequestWhenResponsePredicateRejects() {
        val response = postPlainText("/request-only", "req-body")

        assertThat(response).isEqualTo("response:req-body")

        val traceLogCaptor = argumentCaptor<TraceLog>()
        val traceSpanIdCaptor = nullableArgumentCaptor<TraceSpanId>()
        verify(tracerLogger, timeout(1000).times(1)).infor(
            traceLog = traceLogCaptor.capture(),
            message = any(),
            logContext = any(),
            traceSpanId = traceSpanIdCaptor.capture(),
            additionalLogId = anyOrNull()
        )

        val httpLog = traceLogCaptor.firstValue as ConnectHttpLog
        assertThat(httpLog.reqBody).isEqualTo("req-body")
        assertThat(httpLog.resBody).isNull()
        assertThat(httpLog.status).isNull()
        assertThat(traceSpanIdCaptor.firstValue).isNull()
    }

    @Test
    fun shouldLogOnlyResponseWhenRequestPredicateRejects() {
        val response = postPlainText("/response-only", "req-body")

        assertThat(response).isEqualTo("response:req-body")

        val traceLogCaptor = argumentCaptor<TraceLog>()
        verify(tracerLogger, timeout(1000).times(1)).infor(
            traceLog = traceLogCaptor.capture(),
            message = any(),
            logContext = any(),
            traceSpanId = anyOrNull(),
            additionalLogId = anyOrNull()
        )

        val httpLog = traceLogCaptor.firstValue as ConnectHttpLog
        assertThat(httpLog.reqBody).isNull()
        assertThat(httpLog.resBody).isEqualTo("response:req-body")
        assertThat(httpLog.status).isEqualTo(200)
    }

    @Test
    fun shouldSkipBothLogsWhenAllPredicatesReject() {
        val response = postPlainText("/skip-both", "req-body")

        assertThat(response).isEqualTo("response:req-body")

        verify(tracerLogger, never()).infor(
            traceLog = any(),
            message = any(),
            logContext = anyOrNull(),
            traceSpanId = anyOrNull(),
            additionalLogId = anyOrNull()
        )
    }

    @Test
    fun shouldSupportCustomPredicateForSelectiveLogging() {
        val response = postPlainText(
            path = "/header-controlled",
            body = "req-body",
            skipResponseLogging = true
        )

        assertThat(response).isEqualTo("response:req-body")

        val traceLogCaptor = argumentCaptor<TraceLog>()
        verify(tracerLogger, timeout(1000).times(1)).infor(
            traceLog = traceLogCaptor.capture(),
            message = any(),
            logContext = any(),
            traceSpanId = anyOrNull(),
            additionalLogId = anyOrNull()
        )

        val httpLog = traceLogCaptor.firstValue as ConnectHttpLog
        assertThat(httpLog.reqBody).isEqualTo("req-body")
        assertThat(httpLog.resBody).isNull()
        assertThat(httpLog.status).isNull()
    }

    private fun postPlainText(
        path: String,
        body: String,
        skipResponseLogging: Boolean = false
    ): String {
        return WebClient.builder()
            .baseUrl("http://localhost:$port")
            .filter(tracerExchangeFilter)
            .build()
            .post()
            .uri(path)
            .contentType(MediaType.TEXT_PLAIN)
            .apply {
                if (skipResponseLogging) {
                    header("X-Skip-Response", "true")
                }
            }
            .bodyValue(body)
            .retrieve()
            .bodyToMono(String::class.java)
            .block()!!
    }
}

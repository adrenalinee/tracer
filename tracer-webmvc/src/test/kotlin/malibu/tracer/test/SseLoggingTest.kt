package malibu.tracer.test

import malibu.tracer.TraceSpanId
import malibu.tracer.TracerLogger
import malibu.tracer.io.ResponseHttpLog
import malibu.tracer.io.TraceLog
import malibu.tracer.webmvc.TracerWebMvcConfigurer
import malibu.tracer.webmvc.TracerWebMvcContextApplier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.timeout
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.given
import org.mockito.kotlin.nullableArgumentCaptor
import org.mockito.kotlin.verify
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@SpringBootTest(
    classes = [
        LoggerTestConfiguration::class,
        SseLoggingTest.SseLoggingConfiguration::class
    ],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class SseLoggingTest {

    open class SseLoggingConfiguration : TracerWebMvcConfigurer {
        override fun configureTracerWebMvc(context: TracerWebMvcContextApplier) {
            context.traceResponseBody = true
        }
    }

    @MockitoBean
    lateinit var tracerLogger: TracerLogger

    @LocalServerPort
    var port: Int = 0

    @Test
    fun sseResponseShouldStreamAndBeLogged() {
        given(tracerLogger.isInforEnabled()).willReturn(true)
        given(tracerLogger.isDebugDetailEnabled()).willReturn(true)

        val response = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder()
                .uri(URI("http://localhost:$port/sse"))
                .header("Accept", MediaType.TEXT_EVENT_STREAM_VALUE)
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )

        assertThat(response.statusCode()).isEqualTo(200)
        assertThat(response.body()).contains("data:first")
        assertThat(response.body()).contains("data:second")

        val responseLogCaptor = argumentCaptor<TraceLog>()
        val responseTraceSpanCaptor = nullableArgumentCaptor<TraceSpanId>()
        verify(tracerLogger, timeout(1000).atLeastOnce()).infor(
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
        verify(tracerLogger, atLeastOnce()).deDat(
            traceLog = any(),
            message = detailLogCaptor.capture(),
            traceSpanId = anyOrNull(),
            additionalLogId = anyOrNull()
        )

        assertThat(detailLogCaptor.allValues).contains("HTTP response body: ${responseTraceLog.body}")
        assertThat(detailLogCaptor.allValues).noneMatch { it.startsWith("HTTP response sse event:") }
    }
}

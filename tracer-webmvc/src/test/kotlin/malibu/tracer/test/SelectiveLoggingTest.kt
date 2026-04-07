package malibu.tracer.test

import malibu.tracer.TraceSpanId
import malibu.tracer.TracerLogger
import malibu.tracer.io.RequestHttpLog
import malibu.tracer.io.ResponseHttpLog
import malibu.tracer.io.TraceLog
import malibu.tracer.webmvc.TracerWebMvcConfigurer
import malibu.tracer.webmvc.TracerWebMvcContextApplyer
import malibu.tracer.webmvc.TracerWebMvcLoggingPredicates
import org.assertj.core.api.Assertions.assertThat
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
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.net.URI

@SpringBootTest(
    classes = [
        LoggerTestConfiguration::class,
        SelectiveLoggingTest.SelectiveLoggingConfiguration::class
    ],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class SelectiveLoggingTest {

    open class SelectiveLoggingConfiguration : TracerWebMvcConfigurer {
        override fun configureTracerWebMvc(context: TracerWebMvcContextApplyer) {
            context.traceRequestBody = true
            context.traceResponseBody = true
            context.addRequestTracePredicate(
                TracerWebMvcLoggingPredicates.excludePathPattern("/response-only")
            )
            context.addResponseTracePredicate(
                TracerWebMvcLoggingPredicates.excludePathPattern("/request-only")
            )
            context.addExcludePathPatterns("/skip-both")
        }
    }

    @Autowired
    lateinit var testRestTemplate: TestRestTemplate

    @MockitoBean
    lateinit var tracerLogger: TracerLogger

    @Test
    fun shouldLogOnlyRequestWhenResponsePredicateRejects() {
        given(tracerLogger.isInforEnabled()).willReturn(true)
        given(tracerLogger.isDebugDetailEnabled()).willReturn(false)

        postPlainText("/request-only", "req-body")

        val traceLogCaptor = argumentCaptor<TraceLog>()
        val traceSpanIdCaptor = nullableArgumentCaptor<TraceSpanId>()
        verify(tracerLogger, timeout(1000).times(1)).infor(
            traceLog = traceLogCaptor.capture(),
            message = any(),
            logContext = any(),
            traceSpanId = traceSpanIdCaptor.capture(),
            additionalLogId = anyOrNull()
        )

        assertThat(traceLogCaptor.firstValue).isInstanceOf(RequestHttpLog::class.java)
        assertThat((traceLogCaptor.firstValue as RequestHttpLog).body).isEqualTo("req-body")
        assertThat(traceSpanIdCaptor.firstValue).isNotNull
    }

    @Test
    fun shouldLogOnlyResponseWhenRequestPredicateRejects() {
        given(tracerLogger.isInforEnabled()).willReturn(true)
        given(tracerLogger.isDebugDetailEnabled()).willReturn(false)

        postPlainText("/response-only", "req-body")

        val traceLogCaptor = argumentCaptor<TraceLog>()
        val traceSpanIdCaptor = nullableArgumentCaptor<TraceSpanId>()
        verify(tracerLogger, timeout(1000).times(1)).infor(
            traceLog = traceLogCaptor.capture(),
            message = any(),
            logContext = any(),
            traceSpanId = traceSpanIdCaptor.capture(),
            additionalLogId = anyOrNull()
        )

        assertThat(traceLogCaptor.firstValue).isInstanceOf(ResponseHttpLog::class.java)
        assertThat((traceLogCaptor.firstValue as ResponseHttpLog).body).isEqualTo("response:req-body")
        assertThat(traceSpanIdCaptor.firstValue).isNotNull
    }

    @Test
    fun shouldSkipBothLogsWhenAllPredicatesReject() {
        given(tracerLogger.isInforEnabled()).willReturn(true)
        given(tracerLogger.isDebugDetailEnabled()).willReturn(false)

        postPlainText("/skip-both", "req-body")

        verify(tracerLogger, never()).infor(
            traceLog = any(),
            message = any(),
            logContext = anyOrNull(),
            traceSpanId = anyOrNull(),
            additionalLogId = anyOrNull()
        )
    }

    private fun postPlainText(path: String, body: String) {
        val responseEntity = testRestTemplate.exchange(
            RequestEntity.post(URI(path))
                .contentType(MediaType.TEXT_PLAIN)
                .body(body),
            String::class.java
        )

        assertThat(responseEntity.body).isEqualTo("response:$body")
    }
}

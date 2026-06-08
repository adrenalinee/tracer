package malibu.tracer.test

import malibu.tracer.TraceSpanId
import malibu.tracer.TracerLogger
import malibu.tracer.io.ResponseHttpLog
import malibu.tracer.io.TraceLog
import malibu.tracer.webmvc.TracerWebMvcConfigurer
import malibu.tracer.webmvc.TracerWebMvcContextApplier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.times
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.RequestEntity
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest(
    classes = [
        LoggerTestConfiguration::class,
        Step3ResolvedErrorLoggingTest.ResponseBodyTracedConfiguration::class
    ],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Step3ResolvedErrorLoggingTest {

    open class ResponseBodyTracedConfiguration: TracerWebMvcConfigurer {
        override fun configureTracerWebMvc(context: TracerWebMvcContextApplier) {
            context.traceResponseBody = true
        }
    }

    @Autowired
    lateinit var testRestTemplate: TestRestTemplate

    @MockitoBean
    lateinit var tracerLogger: TracerLogger

    @Test
    fun responseStatusExceptionShouldLogGeneratedErrorBody() {
        doTest("/step3/responseStatusException", HttpStatus.BAD_REQUEST)
    }

    @Test
    fun sendErrorShouldLogGeneratedErrorBody() {
        doTest("/step3/sendError", HttpStatus.INTERNAL_SERVER_ERROR)
    }

    private fun doTest(path: String, status: HttpStatus) {
        given {
            tracerLogger.isInforEnabled()
        }.willReturn(true)

        val responseEntity = testRestTemplate.exchange(
            path,
            HttpMethod.GET,
            RequestEntity.EMPTY,
            String::class.java
        )

        assertThat(responseEntity.statusCode).isEqualTo(status)
        assertThat(responseEntity.body).isNotBlank()

        val traceLogCaptor = argumentCaptor<TraceLog>()
        val logContextCaptor = nullableArgumentCaptor<Any>()
        val traceSpanIdCaptor = nullableArgumentCaptor<TraceSpanId>()
        verify(tracerLogger, times(1)).infor(
            traceLog = traceLogCaptor.capture(),
            message = anyOrNull(),
            logContext = logContextCaptor.capture(),
            traceSpanId = traceSpanIdCaptor.capture(),
            additionalLogId = any()
        )

        val errorCaptor = nullableArgumentCaptor<Throwable>()
        verify(tracerLogger, times(1)).error(
            traceLog = traceLogCaptor.capture(),
            message = anyOrNull(),
            error = errorCaptor.capture(),
            logContext = logContextCaptor.capture(),
            traceSpanId = traceSpanIdCaptor.capture(),
            additionalLogId = any()
        )

        assertRequestTraceLog(traceLogCaptor.firstValue, HttpMethod.GET, path, null)
        assertResponseTraceLog(traceLogCaptor.secondValue, HttpMethod.GET, path, responseEntity.body, status)
        assertThat((traceLogCaptor.secondValue as ResponseHttpLog).body).isEqualTo(responseEntity.body)
        assertLogContext(logContextCaptor)
        assertTraceSpanId(traceSpanIdCaptor)
    }
}

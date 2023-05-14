package malibu.tracer.test

import malibu.tracer.TraceSpanId
import malibu.tracer.TracerLogger
import malibu.tracer.io.TraceLog
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.times
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.RequestEntity

@SpringBootTest(
    classes = *[LoggerTestConfiguration::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Step1LoggingTest {

    @Autowired
    lateinit var testRestTemplate: TestRestTemplate

    @MockBean
    lateinit var tracerLogger: TracerLogger

    private val path = "/step1"

    @Test
    fun getTest() {
        doTest(HttpMethod.GET)
    }

    @Test
    fun postTest() {
        doTest(HttpMethod.POST)
    }

    @Disabled("testRestTemplate 이 patch method 처리하지 않음..")
    @Test
    fun patchTest() {
        doTest(HttpMethod.PATCH)
    }

    @Test
    fun putTest() {
        doTest(HttpMethod.PUT)
    }

    @Test
    fun deleteTest() {
        doTest(HttpMethod.DELETE)
    }

    private fun doTest(method: HttpMethod) {
        given {
            tracerLogger.isInforEnabled()
        }.willReturn(true)

        val responseEntity = testRestTemplate.exchange(
            path,
            method,
            RequestEntity.EMPTY,
            String::class.java
        )

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity.body).isEqualTo(null)

        val traceLogCaptor = argumentCaptor<TraceLog>()
        val logContextCaptor = nullableArgumentCaptor<Any>()
        val traceSpanIdCaptor = nullableArgumentCaptor<TraceSpanId>()
        verify(tracerLogger, times(2)).infor(
            traceLog = traceLogCaptor.capture(),
            message = anyOrNull(),
            logContext = logContextCaptor.capture(),
            traceSpanId = traceSpanIdCaptor.capture(),
            additionalLogId = any()
        )

        assertThat(logContextCaptor.firstValue).isEqualTo(logContextCaptor.secondValue)
        assertRequestTraceLog(traceLogCaptor.firstValue, method, path, null)
        assertResponseTraceLog(traceLogCaptor.secondValue, method, path, null)
        assertLogContext(logContextCaptor)
        assertTraceSpanId(traceSpanIdCaptor)
    }
}
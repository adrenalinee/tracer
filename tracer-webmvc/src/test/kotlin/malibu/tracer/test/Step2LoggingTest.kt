package malibu.tracer.test

import malibu.tracer.TraceSpanId
import malibu.tracer.TracerLogger
import malibu.tracer.io.TraceLog
import malibu.tracer.test.controller.Step2LoggerTestController
import malibu.tracer.webmvc.TracerWebMvcConfigurer
import malibu.tracer.webmvc.TracerWebMvcContextApplyer
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
    classes = *[
        LoggerTestConfiguration::class,
        Step2LoggingTest.RequestBodyTracedConfiguration::class
    ],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Step2LoggingTest {

    open class RequestBodyTracedConfiguration: TracerWebMvcConfigurer {
        override fun configureTracerWebflux(context: TracerWebMvcContextApplyer) {
            context.traceResponseBody = true
        }
    }

    @Autowired
    lateinit var testController: Step2LoggerTestController

    @Autowired
    lateinit var testRestTemplate: TestRestTemplate

    @MockBean
    lateinit var tracerLogger: TracerLogger

    private val path = "/step2"

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
        assertThat(responseEntity.body).isEqualTo(testController.responseBody)

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
        assertResponseTraceLog(traceLogCaptor.secondValue, method, path, testController.responseBody)
        assertLogContext(logContextCaptor)
        assertTraceSpanId(traceSpanIdCaptor)
    }
}
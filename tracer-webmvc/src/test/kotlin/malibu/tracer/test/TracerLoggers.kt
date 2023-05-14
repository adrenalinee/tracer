package malibu.tracer.test

import malibu.tracer.TraceSpanId
import malibu.tracer.io.RequestHttpLog
import malibu.tracer.io.ResponseHttpLog
import malibu.tracer.io.TraceLog
import malibu.tracer.webmvc.ServletExchange
import org.assertj.core.api.Assertions.*
import org.mockito.kotlin.KArgumentCaptor
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

fun assertRequestTraceLog(
    traceLog: TraceLog,
    method: HttpMethod,
    path: String,
    reqBody: String? = null,
) {
    assertThat(traceLog).isInstanceOf(RequestHttpLog::class.java)
    traceLog as RequestHttpLog
    assertThat(traceLog.protocol).isEqualTo("HTTP")
    assertThat(traceLog.method).isEqualTo(method.name())
    assertThat(traceLog.path).isEqualTo(path)
    reqBody?.also {
        assertThat(traceLog.body).isEqualTo(reqBody)
    }
}

fun assertResponseTraceLog(
    traceLog: TraceLog,
    method: HttpMethod,
    path: String,
    resBody: String? = null,
    status: HttpStatus = HttpStatus.OK
) {
    assertThat(traceLog).isInstanceOf(ResponseHttpLog::class.java)
    traceLog as ResponseHttpLog
    assertThat(traceLog.protocol).isEqualTo("HTTP")
    assertThat(traceLog.method).isEqualTo(method.name())
    assertThat(traceLog.path).isEqualTo(path)
    assertThat(traceLog.status).isEqualTo(status.value())
    resBody?.also {
        assertThat(traceLog.body).isEqualTo(resBody)
    }
}

//fun assertTraceLog(
//    traceLogCaptor: KArgumentCaptor<TraceLog>,
//    method: HttpMethod,
//    path: String,
//    status: HttpStatus = HttpStatus.OK,
//    reqBody: String? = null,
//    resBody: String? = null
//) {
//    traceLogCaptor.firstValue.also { traceLog ->
//        assertThat(traceLog).isInstanceOf(RequestHttpLog::class.java)
//        traceLog as RequestHttpLog
//        assertThat(traceLog.protocol).isEqualTo("HTTP")
//        assertThat(traceLog.method).isEqualTo(method.name)
//        assertThat(traceLog.path).isEqualTo(path)
//        reqBody?.also {
//            assertThat(traceLog.body).isEqualTo(reqBody)
//        }
//    }
//
//    traceLogCaptor.secondValue.also { traceLog ->
//        assertThat(traceLog).isInstanceOf(ResponseHttpLog::class.java)
//        traceLog as ResponseHttpLog
//        assertThat(traceLog.protocol).isEqualTo("HTTP")
//        assertThat(traceLog.method).isEqualTo(method.name)
//        assertThat(traceLog.path).isEqualTo(path)
//        assertThat(traceLog.status).isEqualTo(status.value())
//        resBody?.also {
//            assertThat(traceLog.body).isEqualTo(resBody)
//        }
//    }
//}

fun assertLogContext(logContextCaptor: KArgumentCaptor<Any?>) {
    assertThat(logContextCaptor.firstValue).isNotNull
    assertThat(logContextCaptor.secondValue).isNotNull
//    assertThat(logContextCaptor.firstValue).isEqualTo(logContextCaptor.secondValue)
    logContextCaptor.firstValue?.also {
        assertThat(it).isInstanceOf(ServletExchange::class.java)
    }
}

fun assertTraceSpanId(traceSpanIdCaptor: KArgumentCaptor<TraceSpanId?>) {
    assertThat(traceSpanIdCaptor.firstValue).isNotNull
    assertThat(traceSpanIdCaptor.secondValue).isNotNull
    assertThat(traceSpanIdCaptor.firstValue!!.traceId)
        .isEqualTo(traceSpanIdCaptor.secondValue!!.traceId)
}
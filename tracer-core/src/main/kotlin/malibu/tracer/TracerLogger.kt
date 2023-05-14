package malibu.tracer

import malibu.tracer.io.Trace
import malibu.tracer.io.TraceLog
import mu.KotlinLogging
import net.logstash.logback.marker.Markers

class TracerLogger(
    private val tracerContext: TracerContext
) {

    companion object {
        private val traceDataCreatorRegistry = TraceDataCreatorRegistry.getInstance()
    }

    private val tracerLogger = KotlinLogging.logger(TracerContext.TRACER_LOG_NAME)

    private val tracerLogger2 = KotlinLogging.logger(TracerContext.TRACER_DETAIL_LOG_NAME)

    fun isDebugEnabled(): Boolean {
        return tracerLogger.isDebugEnabled
    }

    fun isInforEnabled(): Boolean {
        return tracerLogger.isInfoEnabled
    }

    fun isDebugDetailEnabled(): Boolean {
        return tracerLogger2.isDebugEnabled
    }

    fun isInforDetailEnabled(): Boolean {
        return tracerLogger2.isInfoEnabled
    }


    /**
     *
     */
    fun deDat(traceLog: TraceLog, message: String, traceSpanId: TraceSpanId? = null, additionalLogId: String? = null) {
        tracerLogger2.debug("${genLogId(traceSpanId, additionalLogId)} ${traceLog.type}: $message")
    }

    /**
     * info detail (info() 와 길이를 맞추기 위해 이런 이름을 사용. 길이 가 같으면 로그 보기가 쉬움.
     * es 메시지 없이 단순 메시지만 출력
     */
    fun inDet(traceLog: TraceLog, message: String, traceSpanId: TraceSpanId? = null, additionalLogId: String? = null) {
        tracerLogger2.info("${genLogId(traceSpanId, additionalLogId)} ${traceLog.type}: $message")
    }

    /**
     *
     */
    fun debug(
        traceLog: TraceLog,
        message: String,
        logContext: Any? = null,
        traceSpanId: TraceSpanId? = null,
        additionalLogId: String? = null
    ) {
        tracerLogger.debug(
            Markers.aggregate(
                Markers.appendFields(createTraceBase(traceSpanId)),
                Markers.append(TracerContext.TRACE_FIELD_NAME, traceLog)
            ).apply {
                logContext?.let {
                    traceDataCreatorRegistry.traceLogType(traceLog.type)
                        .create(traceLog.protocol, logContext)
                }?.also { data ->
                    this.add(Markers.append("data", data))
                }
            },
            "${genLogId(traceSpanId, additionalLogId)} ${traceLog.type}: $message"
        )
    }

    /**
     *
     */
    fun infor(
        traceLog: TraceLog,
        message: String,
        logContext: Any? = null,
        traceSpanId: TraceSpanId? = null,
        additionalLogId: String? = null
    ) {
        tracerLogger.info(
            Markers.aggregate(mutableListOf(
                Markers.appendFields(createTraceBase(traceSpanId)),
                Markers.append(TracerContext.TRACE_FIELD_NAME, traceLog)
            ).apply {
                logContext?.let {
                    traceDataCreatorRegistry.traceLogType(traceLog.type)
                        .create(traceLog.protocol, logContext)
                }?.also { data ->
                    this.add(Markers.append("data", data))
                }
            }),
            "${genLogId(traceSpanId, additionalLogId)} ${traceLog.type}: $message"
        )
    }

    /**
     *
     */
    fun error(
        traceLog: TraceLog,
        message: String,
        error: Throwable?,
        logContext: Any? = null,
        traceSpanId: TraceSpanId? = null,
        additionalLogId: String? = null
    ) {
        tracerLogger.error(
            Markers.aggregate(
                Markers.appendFields(createTraceBase(traceSpanId)),
                Markers.append(TracerContext.TRACE_FIELD_NAME, traceLog)
            ).apply {
                logContext?.let {
                    traceDataCreatorRegistry.traceLogType(traceLog.type)
                        .create(traceLog.protocol, logContext)
                }?.also { data ->
                    this.add(Markers.append("data", data))
                }
            },
            "${genLogId(traceSpanId, additionalLogId)} ${traceLog.type}: $message",
            error
        )
    }

    /**
     *
     */
    fun getRunId(): String {
        return tracerContext.runId
    }

    /**
     *
     */
    fun genLogId(traceSpanId: TraceSpanId?, additionalLogId: String? = null): String {
        return if (traceSpanId != null) {
            if (additionalLogId != null) {
                "[${traceSpanId.shortTraceId}][$additionalLogId]"
            } else {
                "[${traceSpanId.shortTraceId}]"
            }
        } else {
            if (additionalLogId != null) {
                "(${tracerContext.runId})[$additionalLogId]"
            } else {
                "(${tracerContext.runId})"
            }
        }
    }

    private fun createTraceBase(traceSpanId: TraceSpanId?): Trace {
        return traceSpanId?.let {
            Trace(
                runId = tracerContext.runId,
                traceId = it.traceId,
                spanId = it.spanId,
                domain = tracerContext.domain
            )
        }?: Trace(
            runId = tracerContext.runId,
            traceId = null,
            spanId = null,
            domain = tracerContext.domain
        )
    }

}
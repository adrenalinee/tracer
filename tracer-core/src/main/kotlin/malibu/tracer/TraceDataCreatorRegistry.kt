package malibu.tracer

import mu.KotlinLogging

class TraceDataCreatorRegistry {

    private val logger = KotlinLogging.logger {}

    /**
     * type, protocol 별로 data creator 저장
     */
    private val traceDataCreators = mutableMapOf<String, MutableMap<String, TraceLogDataCreator<*>>>()

    companion object {
        private val instance = TraceDataCreatorRegistry()

        /**
         * singleton
         */
        fun getInstance(): TraceDataCreatorRegistry {
            return instance
        }
    }

    /**
     *
     */
    private constructor()

    /**
     *
     */
    fun traceLogType(traceLogType: String): TraceLogTypeHolder {
        return TraceLogTypeHolder(traceLogType, this)
    }

    /**
     *
     */
    internal fun register(type: String, protocol: String, dataCreator: TraceLogDataCreator<*>) {
        if (traceDataCreators.containsKey(type).not()) {
            traceDataCreators[type] = mutableMapOf()
        }

        traceDataCreators[type]?.put(protocol, dataCreator)
    }

    /**
     *
     */
    internal fun create(type: String, protocol: String?, logContext: Any): Any? {
        return try {
            traceDataCreators[type]?.let {
                //TODO protocol이 null 일때 처리 필요함.
                it[protocol]?.create(logContext)
            }
        } catch (ex: Exception) {
            logger.warn("TRACER: requestLog data creator에서 에러 발생", ex)
            null
        }
    }
}

/**
 *
 */
class TraceLogTypeHolder(
    private val traceLogType: String,
    private val traceDataCreatorRegistry: TraceDataCreatorRegistry
) {

    /**
     *
     */
    fun register(protocol: String, dataCreator: TraceLogDataCreator<*>) {
        traceDataCreatorRegistry.register(traceLogType, protocol, dataCreator)
    }

    /**
     *
     */
    internal fun create(protocol: String?, logContext: Any): Any? {
        return traceDataCreatorRegistry.create(traceLogType, protocol, logContext)
    }
}
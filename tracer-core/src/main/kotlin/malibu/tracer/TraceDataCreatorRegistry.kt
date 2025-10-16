package malibu.tracer

import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

class TraceDataCreatorRegistry {

    private val logger = KotlinLogging.logger {}

    /**
     * type, protocol 별로 data creator 저장
     */
    private val traceDataCreators: ConcurrentHashMap<String, ConcurrentHashMap<String, TraceLogDataCreator<*>>> =
        ConcurrentHashMap()

    companion object {
        private const val DEFAULT_PROTOCOL_KEY = "__default__"
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
    internal fun register(type: String, protocol: String?, dataCreator: TraceLogDataCreator<*>) {
        val protocolKey = protocol ?: DEFAULT_PROTOCOL_KEY
        val creatorsForType = traceDataCreators.computeIfAbsent(type) {
            ConcurrentHashMap<String, TraceLogDataCreator<*>>()
        }
        creatorsForType[protocolKey] = dataCreator
    }

    /**
     *
     */
    internal fun create(type: String, protocol: String?, logContext: Any): Any? {
        return try {
            val creatorsForType = traceDataCreators[type] ?: return null
            val protocolKey = protocol ?: DEFAULT_PROTOCOL_KEY
            creatorsForType[protocolKey]?.create(logContext)
                ?: if (protocol != null) {
                    creatorsForType[DEFAULT_PROTOCOL_KEY]?.create(logContext)
                } else {
                    null
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
    fun register(protocol: String? = null, dataCreator: TraceLogDataCreator<*>) {
        traceDataCreatorRegistry.register(traceLogType, protocol, dataCreator)
    }

    /**
     *
     */
    internal fun create(protocol: String?, logContext: Any): Any? {
        return traceDataCreatorRegistry.create(traceLogType, protocol, logContext)
    }
}
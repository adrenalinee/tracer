package malibu.tracer

import malibu.tracer.io.FinishLog
import malibu.tracer.io.StartLog
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class TracerBaseConfiguration(
    private val tracerConfiguerers: List<TracerConfigurer>
) {

    private val logger = KotlinLogging.logger {}

    private val traceDataCreatorRegistry = TraceDataCreatorRegistry.getInstance()

    private val tracerContext = TracerContext()

    private val tracerLogger = TracerLogger(tracerContext)

    @PostConstruct
    fun initialize() {
        if (tracerConfiguerers.isNotEmpty()) {
            tracerConfiguerers.forEach { tracerConfigurer ->
                logger.info { "TRACER: found TracerConfigurer: $tracerConfigurer" }
                val tracerContextApplyer = TracerContextApplyer()

                tracerConfigurer.configureTracer(tracerContextApplyer)
                tracerConfigurer.configureDataCreator(traceDataCreatorRegistry)

                tracerContextApplyer.apply(tracerContext)
            }
        }

        if (tracerLogger.isInforEnabled()) {
            tracerLogger.infor(StartLog(), "application starting...")
        }
    }

    @PreDestroy
    fun onDestroy() {
        if (tracerLogger.isInforEnabled()) {
            tracerLogger.infor(FinishLog(), "application finishing...")
        }
    }

    @Bean
    open fun tracerContext(): TracerContext {
        return tracerContext
    }

    @Bean
    open fun tracerLogger(): TracerLogger {
        return tracerLogger
    }
}
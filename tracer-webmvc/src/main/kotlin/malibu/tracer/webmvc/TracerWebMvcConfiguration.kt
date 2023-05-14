package malibu.tracer.webmvc

import malibu.tracer.TracerBaseConfiguration
import malibu.tracer.TracerContext
import malibu.tracer.TracerLogger
import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Import(TracerBaseConfiguration::class)
@Configuration
open class TracerWebMvcConfiguration(
    private val tracerLogger: TracerLogger,
    private val tracerContext: TracerContext,
    private val tracerWebMvcConfigurers: List<TracerWebMvcConfigurer>
) {

    private val logger = KotlinLogging.logger {}

    private val tracerWebMvcContext = TracerWebMvcContext()

    @PostConstruct
    fun initialize() {
        tracerContext.spanIdGenerator = StdWebMvcSpanIdGenerator()

        if (tracerWebMvcConfigurers.isNotEmpty()) {
            tracerWebMvcConfigurers.forEach { tracerConfigurer ->
                logger.info { "TRACER: found TracerWebMvcConfigurer: $tracerConfigurer" }
                val tracerWebMvcContextApplyer = TracerWebMvcContextApplyer()
                tracerConfigurer.configureTracerWebflux(tracerWebMvcContextApplyer)

                tracerWebMvcContextApplyer.apply(tracerWebMvcContext)
            }
        }
    }

    @Bean
    open fun webMvcTracerLogger(): WebMvcTracerLogger {
        return WebMvcTracerLogger(tracerLogger)
    }

    @Bean
    open fun tracerFilter(): TracerFilter {
        return TracerFilter(tracerLogger, tracerContext, tracerWebMvcContext)
    }
}
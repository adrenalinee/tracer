package malibu.tracer.webflux

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
open class TracerWebfluxConfiguration(
    private val tracerLogger: TracerLogger,
    private val tracerContext: TracerContext,
    private val tracerWebfluxConfigurers: List<TracerWebfluxConfigurer>
) {

    private val logger = KotlinLogging.logger {}

    private val tracerWebFluxContext = TracerWebfluxContext()

    @PostConstruct
    fun initialize() {
        tracerContext.spanIdGenerator = StdWebfluxSpanIdGenerator()

        if (tracerWebfluxConfigurers.isNotEmpty()) {
            tracerWebfluxConfigurers.forEach { tracerConfigurer ->
                logger.info { "TRACER: found TracerWebfluxConfigurer: $tracerConfigurer" }
                val tracerWebFluxContextApplyer = TracerWebfluxContextApplyer()
                tracerConfigurer.configureTracerWebflux(tracerWebFluxContextApplyer)

                tracerWebFluxContextApplyer.apply(tracerWebFluxContext)
            }
        }
    }

    @Bean
    open fun tracerWebFilter(): TracerWebFilter {
        return TracerWebFilter(tracerLogger, tracerContext, tracerWebFluxContext)
    }

    @Bean
    open fun tracerWebExceptionHandler(): TracerWebExceptionHandler {
        return TracerWebExceptionHandler()
    }

    @Bean
    open fun webfluxTracerLogger(): WebfluxTracerLogger {
        return WebfluxTracerLogger(tracerLogger)
    }
}

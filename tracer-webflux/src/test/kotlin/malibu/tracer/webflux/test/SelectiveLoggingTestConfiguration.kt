package malibu.tracer.webflux.test

import malibu.tracer.webflux.TracerWebfluxConfiguration
import malibu.tracer.webflux.TracerWebfluxConfigurer
import malibu.tracer.webflux.TracerWebfluxLoggingPredicates
import malibu.tracer.webflux.test.controller.SelectiveLoggingTestController
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@Import(TracerWebfluxConfiguration::class)
@SpringBootApplication(scanBasePackageClasses = [SelectiveLoggingTestController::class])
open class SelectiveLoggingTestConfiguration {

    @Bean
    open fun tracerWebfluxConfigurer(): TracerWebfluxConfigurer {
        return object : TracerWebfluxConfigurer {
            override fun configureTracerWebflux(context: malibu.tracer.webflux.TracerWebfluxContextApplier) {
                context.traceRequestBody = true
                context.traceResponseBody = true
                context.addRequestTracePredicate(
                    TracerWebfluxLoggingPredicates.excludePathPattern("/response-only")
                )
                context.addResponseTracePredicate(
                    TracerWebfluxLoggingPredicates.excludePathPattern("/request-only")
                )
                context.addExcludePathPatterns("/skip-both")
            }
        }
    }
}

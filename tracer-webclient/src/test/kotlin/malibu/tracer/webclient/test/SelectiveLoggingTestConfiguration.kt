package malibu.tracer.webclient.test

import malibu.tracer.webclient.TracerWebClientConfiguration
import malibu.tracer.webclient.TracerWebClientConfigurer
import malibu.tracer.webclient.TracerWebClientContextApplyer
import malibu.tracer.webclient.TracerWebClientLoggingPredicates
import malibu.tracer.webclient.test.controller.SelectiveLoggingTestController
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@Import(TracerWebClientConfiguration::class)
@SpringBootApplication(scanBasePackageClasses = [SelectiveLoggingTestController::class])
open class SelectiveLoggingTestConfiguration {

    @Bean
    open fun tracerWebClientConfigurer(): TracerWebClientConfigurer {
        return object : TracerWebClientConfigurer {
            override fun configureTracerWebClient(context: TracerWebClientContextApplyer) {
                context.traceRequestBody = true
                context.traceResponseBody = true
                context.addRequestTracePredicate(
                    TracerWebClientLoggingPredicates.excludePathPattern("/response-only")
                )
                context.addResponseTracePredicate(
                    TracerWebClientLoggingPredicates.excludePathPattern("/request-only")
                )
                context.addExcludePathPatterns("/skip-both")
                context.addResponseTracePredicate { request ->
                    request.headers().getFirst("X-Skip-Response") != "true"
                }
            }
        }
    }
}

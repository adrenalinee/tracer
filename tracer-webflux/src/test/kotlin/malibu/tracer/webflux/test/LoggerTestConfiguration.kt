package malibu.tracer.webflux.test

import malibu.tracer.webflux.TracerWebfluxConfiguration
import malibu.tracer.webflux.TracerWebfluxConfigurer
import malibu.tracer.webflux.test.controller.SseLoggingTestController
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@Import(TracerWebfluxConfiguration::class)
@SpringBootApplication(scanBasePackageClasses = [SseLoggingTestController::class])
open class LoggerTestConfiguration {

    @Bean
    open fun tracerWebfluxConfigurer(): TracerWebfluxConfigurer {
        return object : TracerWebfluxConfigurer {
            override fun configureTracerWebflux(context: malibu.tracer.webflux.TracerWebfluxContextApplyer) {
                context.traceResponseBody = true
            }
        }
    }
}

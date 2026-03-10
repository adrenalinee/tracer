package malibu.tracer.webflux.test

import malibu.tracer.webflux.TracerWebfluxConfiguration
import malibu.tracer.webflux.TracerWebfluxConfigurer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@Import(TracerWebfluxConfiguration::class)
@SpringBootApplication
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

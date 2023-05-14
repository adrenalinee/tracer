package malibu.tracer.test

import malibu.tracer.webmvc.TracerWebMvcConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import

@Import(TracerWebMvcConfiguration::class)
@SpringBootApplication
open class LoggerTestConfiguration {

//    @Bean
//    open fun loggerTestController(): LoggerTestController {
//        return LoggerTestController()
//    }
}
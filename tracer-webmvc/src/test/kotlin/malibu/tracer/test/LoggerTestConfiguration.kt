package malibu.tracer.test

import malibu.tracer.webmvc.TracerWebMvcConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.context.annotation.Import

@Import(TracerWebMvcConfiguration::class)
@AutoConfigureTestRestTemplate
@SpringBootApplication
open class LoggerTestConfiguration {

//    @Bean
//    open fun loggerTestController(): LoggerTestController {
//        return LoggerTestController()
//    }
}
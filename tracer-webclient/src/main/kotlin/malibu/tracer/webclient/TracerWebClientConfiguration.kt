package malibu.tracer.webclient

import malibu.tracer.TracerBaseConfiguration
import malibu.tracer.TracerLogger
import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Import(TracerBaseConfiguration::class)
@Configuration
open class TracerWebClientConfiguration(
    private val tracerLogger: TracerLogger,
    private val tracerWebClientConfigurers: List<TracerWebClientConfigurer>
) {

    private val logger = KotlinLogging.logger {}

    private val tracerWebClientContext: TracerWebClientContext = TracerWebClientContext()

    @PostConstruct
    fun initialize() {
        tracerWebClientConfigurers.forEach { webClientConfigurer ->
            logger.info { "TRACER: found TracerWebClientConfigurer: $webClientConfigurer" }
            val webClientContextApplyer = TracerWebClientContextApplyer()
            webClientConfigurer.configureTracerWebClient(webClientContextApplyer)
            webClientContextApplyer.apply(tracerWebClientContext)
        }
    }

    @Bean
    open fun tracerExchangeFilter(): TracerExchangeFilter {
        return TracerExchangeFilter(tracerLogger, tracerWebClientContext)
    }

    @Bean
    open fun tracerWebMvcSupportFilter(ac: ApplicationContext): TracerWebMvcSupportFilter {
        return try {
            val webMvcTracerLogger = ac.getBean(malibu.tracer.webmvc.WebMvcTracerLogger::class.java)
            val filter = TracerWebMvcSupportFilter()
            filter.webMvcTracerLogger = webMvcTracerLogger
            filter
        } catch (ex: Throwable) {
            //webMvc 기반에서 실행된것이 아니라서 webMvc support 를 할 필요 없음.
            //그 경우 여기서 리턴하는 filter 는 사용할 곳이 없고 정상동작 하지 않음.
            TracerWebMvcSupportFilter()
        }
    }
}
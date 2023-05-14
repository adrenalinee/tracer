package malibu.tracer.task

import malibu.tracer.TracerBaseConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Import(TracerBaseConfiguration::class)
@Configuration
open class TracerTaskConfiguration {
}
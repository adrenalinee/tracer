package malibu.tracer.logstash

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import net.logstash.logback.decorate.JsonFactoryDecorator

class TracerJsonFactoryDecorator: JsonFactoryDecorator {

    override fun decorate(factory: JsonFactory): JsonFactory {
        (factory.codec as ObjectMapper).setSerializationInclusion(JsonInclude.Include.NON_NULL)
        return factory
    }
}
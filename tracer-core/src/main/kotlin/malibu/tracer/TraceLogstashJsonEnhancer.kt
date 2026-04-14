package malibu.tracer

import ch.qos.logback.classic.spi.ILoggingEvent
import com.google.cloud.spring.logging.JsonLoggingEventEnhancer
import net.logstash.logback.marker.LogstashMarker
import org.slf4j.Marker
import tools.jackson.databind.json.JsonMapper
import java.io.StringWriter

class TraceLogstashJsonEnhancer : JsonLoggingEventEnhancer {

    private val jsonMapper = JsonMapper.builder()
        .build()

    override fun enhanceJsonLogEntry(jsonMap: MutableMap<String, Any>, event: ILoggingEvent) {
        event.markerList?.forEach { marker ->
            mergeMarker(jsonMap, marker)
        }
    }

    private fun mergeMarker(jsonMap: MutableMap<String, Any>, marker: Marker) {
        if (marker is LogstashMarker) {
            runCatching {
                jsonMap.putAll(writeMarker(marker))
            }
        }

        if (marker.hasReferences()) {
            val iterator = marker.iterator()
            while (iterator.hasNext()) {
                mergeMarker(jsonMap, iterator.next())
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun writeMarker(marker: LogstashMarker): Map<String, Any> {
        val writer = StringWriter()

        jsonMapper.createGenerator(writer).use { generator ->
            generator.writeStartObject()
            marker.writeTo(generator)
            generator.writeEndObject()
        }

        return jsonMapper.readValue(writer.toString(), MutableMap::class.java) as Map<String, Any>
    }
}

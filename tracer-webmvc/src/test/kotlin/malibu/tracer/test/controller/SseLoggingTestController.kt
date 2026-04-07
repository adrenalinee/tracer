package malibu.tracer.test.controller

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.CompletableFuture

@RestController
class SseLoggingTestController {

    @GetMapping("/sse", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getSse(): SseEmitter {
        val emitter = SseEmitter(10_000L)

        CompletableFuture.runAsync {
            try {
                emitter.send(SseEmitter.event().id("1").name("message").data("first"))
                emitter.send(SseEmitter.event().id("2").name("message").data("second"))
                emitter.complete()
            } catch (ex: Exception) {
                emitter.completeWithError(ex)
            }
        }

        return emitter
    }
}

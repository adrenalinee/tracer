package malibu.tracer.webflux.test.controller

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class ErrorLoggingTestController {

    @GetMapping("/response-status-exception")
    fun responseStatusException(): String {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "response status error")
    }

    @GetMapping("/unhandled-error")
    fun unhandledError(): String {
        throw RuntimeException("unhandled error")
    }
}

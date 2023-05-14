package malibu.tracer.test.controller

import malibu.tracer.test.exception.HandingException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/step3")
class Step3LoggerTestController {

    val responseBody = "tested!"

    val badRequestResponseBody = "bad request!"

//    val serverErrorResponseBody = "serverError!"

    @GetMapping
    fun getTest(@RequestBody reqBody: String?): String {
        return responseBody
    }

    @PostMapping
    fun postTest(@RequestBody reqBody: String?): String {
        return responseBody
    }

    @PatchMapping
    fun patchTest(@RequestBody reqBody: String?): String {
        return responseBody
    }

    @PutMapping
    fun putTest(@RequestBody reqBody: String?): String {
        return responseBody
    }

    @DeleteMapping
    fun deleteTest(@RequestBody reqBody: String?): String {
        return responseBody
    }

    @GetMapping("/handledError")
    fun get400Test(@RequestBody reqBody: String?): String {
        throw HandingException()
    }

    @PostMapping("/handledError")
    fun post400Test(@RequestBody reqBody: String?): String {
        throw HandingException()
    }

    @PatchMapping("/handledError")
    fun patch400Test(@RequestBody reqBody: String?): String {
        throw HandingException()
    }

    @PutMapping("/handledError")
    fun put400Test(@RequestBody reqBody: String?): String {
        throw HandingException()
    }

    @DeleteMapping("/handledError")
    fun delete400Test(@RequestBody reqBody: String?): String {
        throw HandingException()
    }

    @GetMapping("/unhandledError")
    fun get500Test(@RequestBody reqBody: String?): String {
        throw RuntimeException()
    }

    @PostMapping("/unhandledError")
    fun post500Test(@RequestBody reqBody: String?): String {
        throw RuntimeException()
    }

    @PatchMapping("/unhandledError")
    fun patch500Test(@RequestBody reqBody: String?): String {
        throw RuntimeException()
    }

    @PutMapping("/unhandledError")
    fun put500Test(@RequestBody reqBody: String?): String {
        throw RuntimeException()
    }

    @DeleteMapping("/unhandledError")
    fun delete500Test(@RequestBody reqBody: String?): String {
        throw RuntimeException()
    }

    @ExceptionHandler(HandingException::class)
    fun exceptionHandle(ex: HandingException): ResponseEntity<String> {
        return ResponseEntity.badRequest()
            .body(badRequestResponseBody)
    }
}
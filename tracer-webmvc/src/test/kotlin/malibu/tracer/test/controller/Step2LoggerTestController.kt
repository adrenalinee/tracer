package malibu.tracer.test.controller

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/step2")
class Step2LoggerTestController {

    val responseBody = "tested!"

    @GetMapping
    fun getTest(): String {
        return responseBody
    }

    @PostMapping
    fun postTest(): String {
        return responseBody
    }

    @PatchMapping
    fun patchTest(): String {
        return responseBody
    }

    @PutMapping
    fun putTest(): String {
        return responseBody
    }

    @DeleteMapping
    fun deleteTest(): String {
        return responseBody
    }
}
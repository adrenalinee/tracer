package malibu.tracer.test.controller

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/step1")
class Step1LoggerTestController {

    @GetMapping
    fun getTest() {
    }

    @PostMapping
    fun postTest() {
    }

    @PatchMapping
    fun patchTest() {
    }

    @PutMapping
    fun putTest() {
    }

    @DeleteMapping
    fun deleteTest() {
    }
}
package malibu.tracer.test

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    classes = [LoggerTestConfiguration::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Tester {

    @Test
    fun test() {}
}
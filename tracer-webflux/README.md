# Tracer Webflux

spring-webflux 기반의 application에서 http request 와 http response를 로깅할 수 있습니다.
http request 발생시점에 traceId를 발급합니다.
해당 request 가 처리되는 도중에 발생하는 모든 로그는 같은 traceId를 가지게 됩니다.
request body, response body 포함한 http 에 대한 상세정보를 로깅할 수 있습니다.



# Managed log type
- **REQUEST**: http request 처리 시작 시점에 로그를 출력
- **RESPONSE**: http request 처리 후에 응답(response) 직전에 로그를 출력



# Tracer webflux 사용
tracer webflux 를 사용하려면 `TracerWebfluxConfiguration` 을 application에 import 해야 합니다.

```kotlin
@Import(TracerWebfluxConfiguration::class)
@SpringBootApplication
class MyApplication
```

위와 같이 `TracerWebfluxConfiguration` 을 import 하면 http request, response 가 로그로 출력됩니다.



# Tracer webflux configuration
`TracerConfigurer`를 구현하여 tracer-core 설정변경을 할 수 있습니다.

```kotlin
@Component
class MyTracerConfiguration: TracerWebfluxConfigurer {
    override fun configureTracerWebflux(tracerWebfluxContext: TracerWebfluxContextApplyer) {
        // TODO
    }
}
```

## `tracerWebfluxContext`

- **traceRequestBody**: reqeuest body를 출력할지 결정, **type**: boolean, **default**: false
- **traceResponseBody**: response body를 출력할지 결정, **type**: boolean, **default**: false
- **traceRequestHeaders**: rquest header 를 출력할지 결정, **type**: boolean, **default**: false
- **traceResponseHeaders**: response header 를 출력할지 결정, **type**: boolean, **default**: false
- **excludePathPaterns**: trace log 를 출력하지 않을 request path 를 등록, **type**: array(string), **default**: []



## 주의 사항
request/response body를 출력하게 되면 application 실행에 약간이나마 부담을 주게 됩니다.
body를 가로체서 caching을 하게 되는데, 이과정에서 Cpu, memory가 추가적으로 사용됩니다.



# Custom type log
spring-webflux 를 사용한다면 `WebfluxTracerLogger` 를 사용하여 custom log type 을 출력해야 합니다.
`WebfluxTracerLogger` 은 로그를 출력하려는 traceId를 찾아주는 역할을 해줍니다. wbflux 는 reactive stream(projectreactor) 기반
이기때문에 특별한 방식으로 사용해야만 traceId를 찾아올 수 있습니다. (stream context 에 traceId를 저장하고 여기서 꺼내옴)

```kotlin
data class CustomTypeTraceLog(
    val attr1: String,
    val attr2: Int
): TraceLog(
    type = "CUSTOM_LOG_TYPE"
)

@Component
class CustomTraceLogger(
    private val webfluxTracerLogger: WebfluxTracerLogger
) {
    fun info(customTypeTraceLog: CustomTypeTraceLog): Mono<Void> {
        return webfluxTracerLogger.infor(
            traceLog = customTypeTraceLog, //JSON 포맷 로그에 출력될 객체
            message = "console 용 메시지 입니다. " +
                "attr1: ${customTypeTraceLog.attr1}, " +
                "attr2: ${customTypeTraceLog.attr2}" //TEXT 포맷 로그에 출력될 문자
        )
    }
}

@Component
class UserService(
    private val customTraceLogger: CustomTraceLogger
) {
    fun someAction(): Mono<String> {
        return Mono.just("return value")
            .flatMap { res ->
                customTraceLogger.info(
                    customTypeTraceLog = CustomTypeTraceLog(
                        attr1 = "attr1",
                        attr2 = 2
                    )
                ).then(Mono.just(res))
            }
    }
}
```
위와 같이 `flatMap` 안에서 로그를 출력하고 `.then(Mono.Just(res))`와 같이 추가해주면 기존 동작에 영향을 미치미 않고,
traceId를 가져오면서 로그 출력가능.




# TRACER_DETAIL_LOG logger
request body, response body 를 출력합니다.
다만 request body는 `tracerWebfluxContext.traceRequestBody = true`,
response body 는 request body는 `tracerWebfluxContext.traceResponseBody = true`로 설정해주어야 출력이 가능합니다.
이 설정을 통해서 실제로 body 캐싱을 하게 됩니다.



# Log sample

## REQUEST(http) type
### TRACER_LOG logger
#### TEXT 포맷
```
2022-07-12 12:36:07.932  INFO 15694 --- [ctor-http-nio-3] TRACER_LOG : [202acd7e-8] REQUEST: HTTP request: GET http://localhost:8080/hello
```

#### JSON 포맷
```json
{"sequence":2,"@timestamp":"2022-07-12T12:36:52.694+09:00","level":"INFO","logger_name":"TRACER_LOG","threadName":"reactor-http-nio-3","runId":"4ff5a879-6","traceId":"7feca44a-9942-4f67-a441-aaecdf947735","domain":"hello","trace":{"type":"REQUEST","protocol":"HTTP","method":"GET","url":"http://localhost:8080/hello","path":"/hello"},"@version":"1"}
```

### TRACER_DETAIL_LOG
```
2022-07-12 14:53:01.452 DEBUG 17712 --- [ctor-http-nio-3] TRACER_DETAIL_LOG : [202acd7e-8] REQUEST: HTTP request body: ~~~~
```


## RESPONSE(http) type
### TRACER_LOG logger
#### TEXT 포맷
```
2022-07-12 12:36:08.003  INFO 15694 --- [ctor-http-nio-3] TRACER_LOG : [202acd7e-8] RESPONSE: HTTP response: 200 OK
```

#### JSON 포맷
```json
{"sequence":3,"@timestamp":"2022-07-12T12:36:52.808+09:00","level":"INFO","logger_name":"TRACER_LOG","threadName":"reactor-http-nio-3","runId":"4ff5a879-6","traceId":"7feca44a-9942-4f67-a441-aaecdf947735","domain":"hello","trace":{"type":"RESPONSE","protocol":"HTTP","elapsedTime":120,"method":"GET","status":200,"url":"http://localhost:8080/hello","path":"/hello","body":"hello!"},"@version":"1"}
```


### TRACER_DETAIL_LOG
```
2022-07-12 14:53:01.529 DEBUG 17712 --- [ctor-http-nio-3] TRACER_DETAIL_LOG : [202acd7e-8] RESPONSE: HTTP response body: ~~~~
```




# Known issues

입력된 request body가 있는데 controller에서 request body를 읽지 않는다면 REQEST 로그가 출력되지 않는 문제가 있습니다. 


```kotlin
@RestController
class HelloController {
    @GetMapping("/hello")
    fun getHello(): Mono<String> {
        //TODO
    }
}
```

위의 hello get mapping handler 로 처리되는 아래와 같은 요청이 있다고 했을때,

````
curl --location --request GET 'http://localhost:8080/hello' \
--header 'Content-Type: text/plain' \
--data-raw 'this is reqest body!'
```

현재 이 요청의 REQEST 로그가 출력이 되지 않습니다. 이문제는 모든 http method(GET, POST, ..)에서 발생합니다. 이유는 request body read onComplete event 에서 request body 를 출력하도록 개발되어 있는데, controller에서 body를 읽지 않는다면 request body read onComplete event 가 발생하지 않기때문입니다. 다음버전에서 해결 예정입니다.
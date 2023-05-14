# Tracer Webmvc

spring-web 기반의 application에서 http request 와 http response를 로깅할 수 있습니다.
http request 발생시점에 traceId를 발급합니다.
해당 request 가 처리되는 도중에 발생하는 모든 로그는 같은 traceId를 가지게 됩니다.
request body, response body 포함한 http 에 대한 상세정보를 로깅할 수 있습니다.


# Managed log type
 - **REQUEST**: http request 처리 시작 시점에 로그를 출력
 - **RESPONSE**: http request 처리 후에 응답(response) 직전에 로그를 출력



# Tracer webmvc 사용
tracer webmvc 를 사용하려면 `TracerWebMvcConfiguration` 을 application에 import 해야 합니다.

```kotlin
@Import(TracerWebMvcConfiguration::class)
@SpringBootApplication
class MyApplication
```

위와 같이 `TracerWebMvcConfiguration` 을 import 하면 http request, response 가 로그로 출력됩니다.

# Tracer webmvc configuration
`TracerConfigurer`를 구현하여 tracer-core 설정변경을 할 수 있습니다.

```kotlin
@Component
class MyTracerConfiguration: TracerWebMvcConfigurer {
    override fun configureTracerWebflux(tracerWebMvcContext: TracerWebMvcContextApplyer) {
        // TODO
    }
}
```

## `tracerWebMvcContext`
 - **traceRequestBody**: reqeuest body를 출력할지 결정, **type**: boolean, **default**: false
 - **traceResponseBody**: response body를 출력할지 결정, **type**: boolean, **default**: false
 - **traceRequestHeaders**: rquest header 를 출력할지 결정, **type**: boolean, **default**: false
 - **traceResponseHeaders**: response header 를 출력할지 결정, **type**: boolean, **default**: false
 - **excludePathPaterns**: trace log 를 출력하지 않을 request path 를 등록, **type**: array(string), **default**: []



## 주의 사항
request/response body를 출력하게 되면 application 실행에 약간이나마 부담을 주게 됩니다.
body를 가로체서 caching을 하게 되는데, 이과정에서 Cpu, memory가 추가적으로 사용됩니다.





# Custom type log
spring-webmvc 를 사용한다면 `WebMvcTracerLogger` 를 사용하여 custom log type 을 출력해야 합니다.
`WebMvcTracerLogger` 은 로그를 출력하려는 traceId를 찾아주는 역할을 해줍니다.

```kotlin
data class CustomTypeTraceLog(
    val attr1: String,
    val attr2: Int
): TraceLog(
    type = "CUSTOM_LOG_TYPE"
)

@Component
class CustomTraceLogger(
    private val webMvcTracerLogger: WebMvcTracerLogger
) {
    fun info(customTypeTraceLog: CustomTypeTraceLog) {
        webMvcTracerLogger.infor(
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
    fun someAction() {
        customTraceLogger.info(
            customTypeTraceLog = CustomTypeTraceLog(
                attr1 = "attr1",
                attr2 = 2
            )
        )
    }
}
```



# TRACER_DETAIL_LOG logger
request body, response body 를 출력합니다.
다만 request body는 `tracerWebMvcContext.traceRequestBody = true`,
response body 는 request body는 `tracerWebMvcContext.traceResponseBody = true`로 설정해주어야 출력이 가능합니다.
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



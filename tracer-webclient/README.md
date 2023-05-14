# Tracer webclient

application 에서 spring webClient 를 사용할때 http call 에 대한 정보를 로깅할 수 있습니다.

일반적으로 http server application 에서 서버로 수신된 http request 를 처리하는 도중에 webClient 를 사용하게 됩니다.
이럴때 webClient의 로그 메시지는 서버로 수신된 http request 에 종속적이게 됩니다.

tracer-webclient는 tracer-webmvc, tracer-webflux 와 연동하여 traceId를 공유합니다.
이런 방식을 통해 어떤 http request 를 처리하는 도중에 webClient 사용했는지 확인이 가능합니다.



# Managed log type
- **CONNECT**: http call 에 대한 응답을 받은 시점에 request, response 정보를 로그로 같이 출력




# Tracer webclient 사용
tracer webclient 를 사용하려면 `TracerWebClientConfiguration` 을 application에 import 해야 합니다.

```kotlin
@Import(TracerWebClientConfiguration::class)
@SpringBootApplication
class MyApplication

@Component
class MyComponent(
    val tracerExchangeFilter: TracerExchangeFilter
) {
    private val webClient: WebClient = WebClient.builder()
        .exchangeFunction(tracerExchangeFilter)
}

```
위와 같은 간단한 코드를 추가하면 http request, response 가 로그로 출력됩니다.

추가한 코드 설명:
 1. `TracerWebClientConfiguration` 을 spring configuration 으로 import 하면 `tracerExchangeFilter` 가 spring bean 으로 등록됨.
 2. `webClient` 생성할때 `tracerExchangeFilter` 를 exchangeFunction 으로 추가





# Tracer webclient configuration
`TracerWebClientConfigurer`를 구현하여 tracer-core 설정변경을 할 수 있습니다.

```kotlin
@Component
class MyTracerConfiguration: TracerWebClientConfigurer {
    override fun configureTracerWebClient(tracerWebClientContext: TracerWebClientContextApplyer) {
        // TODO
    }
}
```

## `tracerWebClientContext`

- **traceRequestBody**: reqeuest body를 출력할지 결정, **type**: boolean, **default**: false
- **traceResponseBody**: response body를 출력할지 결정, **type**: boolean, **default**: false
- **traceRequestHeaders**: rquest header 를 출력할지 결정, **type**: boolean, **default**: false
- **traceResponseHeaders**: response header 를 출력할지 결정, **type**: boolean, **default**: false




# TRACER_DETAIL_LOG logger
http call 에 대해서 request body, response body 를 각 각 한줄로 출력합니다.
다만 request body는 `tracerWebClientContext.traceRequestBody = true`,
response body 는 request body는 `tracerWebClientContext.traceResponseBody = true`로 설정해주어야 출력이 가능합니다.
이 설정을 통해서 실제로 body 캐싱을 하게 됩니다.




# Log sample
## CONNECT(http) type
### TRACER_LOG logger
#### TEXT 포맷
```
2022-07-12 12:45:48.268  INFO 16006 --- [ctor-http-nio-3] TRACER_LOG : [2d8471a2-7] CONNECT: HTTP GET: https://search.daum.net/search?w=tot&q=23 200
```


#### JSON 포맷
```json
{"sequence":3,"@timestamp":"2022-07-12T12:44:52.162+09:00","level":"INFO","logger_name":"TRACER_LOG","threadName":"reactor-http-nio-3","runId":"112f0d12-9","traceId":"9ee6aff1-ed76-4aae-8669-d866ae24c49d","domain":"hello","trace":{"type":"CONNECT","protocol":"HTTP","elapsedTime":1268,"status":200,"url":"https://search.daum.net/search?w=tot&q=23","method":"GET","path":"/search"},"@version":"1"}
```


### TRACER_DETAIL_LOG
```
2022-07-12 15:06:24.643 DEBUG 17712 --- [ctor-http-nio-3] TRACER_DETAIL_LOG  : [d3540e57-4][7f36617c-3] CONNECT: HTTP request body: ~~~~
2022-07-12 15:06:24.644 DEBUG 17712 --- [ctor-http-nio-3] TRACER_DETAIL_LOG : [d3540e57-4][7f36617c-3] CONNECT: HTTP response body: ~~~~

```



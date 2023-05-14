# Tracer core

**Tracer**를 사용하기 위한 기본적인 설정과 확장을 관리하는 framework core 모듈입니다.
다른 모든 tracer 모듈은 tracer-core를 기반으로 log type을 확장하는 형태로 개발되어 있습니다.

tracer-core는 기본적으로 application 시작과 끝을 로깅합니다.
application 시작할때 runId를 발급합니다.
그리고 application 종료시까지 발생하는 모든 로그는 동일한 runId를 가지게 됩니다.



## 지원하는 log format

### TEXT format
중요 정보만 plan text 방식으로 출력하고 싶을때 사용합니다. 주로 console 출력 목적으로 사용합니다.
개발과정에서 application의 실행 흐름을 로그를 통해 확인하고 싶을때 활용할 수 있습니다.
TEXT 포맷은 아래 필드들이 기본 출력되니다.
 - traceId or runId: traceId가 있으면 출력되고 traceId가 없는 상황이라면 runId가 출력됨.
 - log type: REQUEST, CONNECT, RESPONSE 등 로그 타입 출력됨.
 - message: log type 별로 디버깅에 활용할 메시지가 출력됨.



### JSON format
json 포맷으로 전체 로그 데이터들을 출력하고 싶을때 사용합니다. 조로 es 전달 목적으로 사용합니다.
logstach-logback-encoder 를 기반으로 json 데이터를 생성합니다.
JSON 포맷은 es를 위한 필수 필드들과 아래의 필드들을 기본적으로 포함합니다.
 - runId: application 시작할때 한번 발급되는 id 로, application 종료할때까지 발생하는 모든 trace log 는 같은 runId를 사용.
 - traceId: REQUEST type 로그가 출력될때 발급되는 id 로, 해당 REQUEST 가 끝나는 동한 발생하는 모든 trace log 는 같은 traceId를 사용.
 - spanId: 하나의 요청이 여러 서버를 걸쳐서 실행되었을 경우에 서버단위로 발급되는 id로, 1에서부터 숫자 증가됨.
 - domain: 여러서버에서 es 로 로그를 수집할때 각 서버를 구별하기 위한 어플리케이션 구분자.




# Managed log type
 - **START**: application 로딩중에 로그를 출력
 - **FINISH**: application 종료중에 로그를 출력



# Tracer-core 사용
Tracer 를 사용하려면 `TracerBaseConfiguration`를 spring 기반 application에 import 해야 합니다.

```kotlin
@Import(TracerBaseConfiguration::class)
@SpringBootApplication
class MyApplication
```

위와 같이 `TracerBaseConfiguration` 를 import 하면 application start, finish 로그가 출력됩니다.



# Tracer configuration
`TracerConfigurer`를 구현하여 tracer-core 설정변경을 할 수 있습니다.

```kotlin
@Component
class MyTracerConfiguration: TracerConfigurer {
    override fun configureTracer(tracerContext: TracerContextApplyer) {
        //TODO 
    }

    override fun configureDataCreator(dataCreatorRegistry: TraceDataCreatorRegistry) {
        //TODO 
    }
}
```

## `tracerContext`
 - **domain**: JSON 포맷 로그 메시지를 es 에서 취합할때 각 서버단위 application을 구분지어 줄 수 있는 application 이름의 용도, **type**: string, **default**: null
 - **useSpan**: span 기능을 사용할지 여부. span은 여러서버에 걸처서 하나의 traceId를 사용하게 할 수 있는 기능, **type**: boolean, **default**: false
 - **traceIdGenerator**: traceId 생성자를 지정함. REQUEST 당 하나의 traceId가 발급됨, **type**: `TraceIdGenerator<*>`, **default**: `SimpleTraceIdGenerator()` (random UUID 생성)
 - **spanIdGenerator**: spanId 생성자를 지정함. 하나의 traceId에 대해서 서버별로 spanId가 발급됨, **type**: `SpanIdGenerator<*>`, **default**: `SimpleSpanIdGenerator()` (1 부터 서버순서대로 숫자 증가)


## `dataCreatorRegistry`
이미 정의되어 있는 log type 의 JSON 포맷 로그 데이터에 추가 field를 넣고 싶을때 사용합니다.
tracer 에서 자체 포맷을 가지고 출력하는 log type 에 개발자가 임의의 필드를 넣을 수 있습니다.

```kotlin
// custom field가 정의된 data class
data class CustomTraceData {
    val custom1: String? = null
}

@Component
class MyTracerConfiguration: TracerConfigurer {

    override fun configureDataCreator(registry: TraceDataCreatorRegistry) {
        egistry.traceLogType(DefinedTraceLogType.REQUEST)
            .register(DefinedProtocolType.HTTP, object: TraceLogDataCreator<CustomTraceData> {
                override fun create(logContext: Any): CustomTraceData? {
                    //TODO logContext 객체에서 필요한 값들을 추출해서 위에서 정의한 CustomTraceData 에 채워준후 return
                    return CustomTraceData()
                }
            })
    }
}
```

위와 같이 설정하면 JSON 포맷 포맷 로그에 http REQUEST type 로그의 data 필드에 위에서 return한 `CustomTraceData` 객체가 json 포함됩니다.
최소 설정의 경우에 아래와 비슷하게 출력됩니다.
```json
{
    "type": "LOG_TYPE",
    "data": {
        "custom1": "custom1 value"
    }
}
```


# `TracerLogger`
Tracer에서 spring bean 으로 제공하는 로그 출력 객체 입니다.
보통은 tracer 모듈에서 사용하고 있지만, 사용자 정의 log type 을 만들 경우에 사용할 수 도 있습니다.
세가지 레벨의 로그를 출력할 수 있습니다. (DEBUG, INFO, ERROR) 로그 레벨은 logback 에서 정의한 로그 레벨과 일치합니다.



# Logback 설정
로그백 설정을 통해서 tracer log 가 출력되게 할 수 있습니다.
기본적으로 중요 로그는 모두 TRACER_LOG logger 로 출력됩니다.
그리고 TRACER_DETAIL_LOG logger 를 통해 추가적으로 필요한 로그가 출력됩니다. TRACER_DETAIL_LOG logger 는
TEXT 포맷으로만 추가 정보를 출력하고, tracer 모듈별로 출력하는 정보가 다릅니다.

아래는 TRACER_LOG logger 를 기준으로 설명합니다.

## TEXT 포맷 로그 출력
최소설정으로 아래와 같이 logback.xml 파일을 resources 폴더에 추가하면 TEXT 포맷 로그를 출력할 수 있습니다.
아래 설정으로 오직 TRACER_LOG 만 터미널에 출력됩니다.
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
            <pattern>[%d{HH:mm:ss.SSS}][%-5level][%logger{36}] - %msg%n</pattern>
        </encoder>
    </appender>

    <logger name="TRACER_LOG">
        <level value="INFO" />
        <appender-ref ref="console" />
    </logger>
</configuration>
```



일반적으로는 logback-spring.xml 파일에 아래와 같이 spring에서 제공하는 base.xml 을 포함시키면 됩니다.
그러면 일반적인 로그들과 함께 TRACE_LOG 메시지가 터미널에 출력됩니다. 파일 이름이 logback-spring.xml 인것은 spring에서
설정한 내용들이 로그 설정에 반영될 수 있게 하기 위함입니다. base.xml 파일에서 기본적인 로그 출력설정이 포합되어 있습니다.
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/base.xml"/>
</configuration>
```


## JSON 포맷 로그 출력
logback.xml 파일에 아래와 같이 설정합니다. tracer 에서 제공하는 tracer-console-appender.xml 파일을 통해
TRACER_CONSOLE appender 를 사용하면 JSON 포맷의 로그를 터미널에 출력할 수 있습니다.
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="malibu/tracer/logback/tracer-console-appender.xml" />
    <logger name="TRACER_LOG">
        <level value="INFO" />
        <appender-ref ref="TRACER_CONSOLE" />
    </logger>
</configuration>
```

로그를 파일에 출력하려면 아래와 같이 tracer-file-appender.xml을 통해 TRACER_FILE appender 를 사용하면
JSON 포맷의 로그를 특정 파일에 출력할 수 있습니다.
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="malibu/tracer/logback/tracer-file-appender.xml" />
    <logger name="TRACER_LOG">
        <level value="INFO" />
        <appender-ref ref="TRACER_FILE" />
    </logger>
</configuration>
```



# Custom type log
**Tracer** 에서 지원하는것 이외에 application 만의 특별한 type의 로그 출력을 할 수 있습니다.
기본적인 아이디어는 `TracerLogger`에 `TraceLog`를 상속하는 객체를 전달해서 로깅을 하면 됩니다.

```kotlin
    tracerLogger.infor(
        traceLog = object: TraceLog(type = "CUSTOM_LOG_TYPE") { //JSON 포맷 로그에 포함될 객체
            val attr1 = attr1,
            val attt2 = attr2
        },
        logContext = null,
        message = "console 용 메시지 입니다. attr1: $attr1, attr2: $attr2" //TEXT 포맷 로그에 출력될 문자
    )
```

실제 개발할때는 아래와 같이 customLogger 를 사용을 권장합니다.

```kotlin
data class CustomTypeTraceLog(
    val attr1: String,
    val attr2: Int
): TraceLog(
    type = "CUSTOM_LOG_TYPE"
)

@Component
class CustomTraceLogger(
    private val tracerLogger: TracerLogger
) {
    fun info(customTypeTraceLog: CustomTypeTraceLog) {
        return tracerLogger.infor(
            traceLog = customTypeTraceLog, //JSON 포맷 로그에 출력될 객체
            logContext = null,
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



# Log sample
실제로 출력되는 로그 메시지 출력의 샘플입니다. log type, log format 별로 
## START type
### TEXT format
```
2022-07-12 11:15:20.137  INFO 13794 --- [main] TRACER_LOG : (e1bfbc37-3) START: application starting...
```
### JSON format
```json
{"sequence":1,"@timestamp":"2022-07-12T11:13:59.461+09:00","level":"INFO","logger_name":"TRACER_LOG","threadName":"main","runId":"dfc0657f-3","domain":"hello","trace":{"type":"START"},"@version":"1"}
```


## FINISH type
### TEXT format
```
2022-07-12 11:15:23.164  INFO 13794 --- [ionShutdownHook] TRACER_LOG   : (e1bfbc37-3) FINISH: application finishing...
```
### JSON format
```json
{"sequence":2,"@timestamp":"2022-07-12T11:14:40.280+09:00","level":"INFO","logger_name":"TRACER_LOG","threadName":"SpringApplicationShutdownHook","runId":"dfc0657f-3","domain":"hello","trace":{"type":"FINISH"},"@version":"1"}
```


## custom type log sample
### TEXT format
```
2022-07-12 13:49:51.963  INFO 16006 --- [ctor-http-nio-3] TRACER_LOG : [26690895-d] CUSTOM_LOG_TYPE: console 용 메시지 입니다. attr1: attr1, attr2: 2
```


### JSON format
```json
{"sequence":3,"@timestamp":"2022-07-12T13:52:21.779+09:00","level":"INFO","logger_name":"TRACER_LOG","threadName":"reactor-http-nio-3","runId":"c2da8c56-1","traceId":"4f034582-7e98-45d4-8dd4-31ef1a597414","domain":"hello","trace":{"type":"CUSTOM_LOG_TYPE","attr1":"attr1","attr2":2},"@version":"1"}
```



## custom field sample
custom field는 JSON 포맷에서만 출력됩니다. 아래는 REQUEST(http) 타입 로그에 custom field가 추가된 샘플입니다.

```json
{"sequence":2,"@timestamp":"2022-07-12T12:36:52.694+09:00","level":"INFO","logger_name":"TRACER_LOG","threadName":"reactor-http-nio-3","runId":"4ff5a879-6","traceId":"7feca44a-9942-4f67-a441-aaecdf947735","domain":"hello","trace":{"type":"REQUEST","protocol":"HTTP","method":"GET","url":"http://localhost:8080/hello","path":"/hello"},"data":{"extraField1":"this is extraField 1","extraField2":2},"@version":"1"}
```

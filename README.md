# Tracer
spring 기반 application 의 표준 동작 내역을 로깅 할 수 있도록 도와주는 프레임워크입니다.
표준출력을 위한 단순 텍스트 로깅과 ES 로 보내기 위한 json 포맷의 로깅을 할 수 있습니다. 

> spring 6 이상, spring-boot 3 이상 버전에서 정상동작합니다.

## motivation
application 실행중에 어떤 중요한 이벤트가 발생하거나 특정 액션이 수행되었는지를 확인해야 할 필요가 있습니다.
확인을 위해 로그를 출력하면 좋은데 복잡한 로깅 설정을 해주거나, 직접 로깅을 위한 복잡한 코딩을 해야 합니다.
이럴때 **Tracer** 가 도움을 줄 수 있습니다.
단순한 설정만으로 application 실행중 일반적으로 알고 싶어하는 상황에 대해서 로깅을 해줍니다.

더 나아가 application 로그를 es 로 보내고 kibina를 통해서 application 실행 내역을 분석해야할 수 있습니다.
그럴때 **Tracer** 가 다시 도움을 줄 수 있습니다.
추가적인 단순한 설정만으로 tracer 가 출력하는 로그 메시지를 es 로 보낼 수 있습니다.


## feature
 - spring webmvc, webflux 기반 개발에서 request, response 에 대한 정보를 로깅할 수 있습니다.
 - spring webClient 로 호출하는 http call 에 대한 request, response 정보르 로깅할 수 있습니다.
 - webClient 로깅의 경우에 어떤 request 에서 사용되었는지 확인 가능합니다.
 - 모든 경우에 request/response body 도 로깅이 가능합니다.
 - tracer 에서 출력하는 JSON 포맷 로그 메시지에 사용자 정의 필드를 추가할 수 있습니다.
 - tracer 에서 사용자 정의 타입의 로그 메시지를 출력할 수 있습니다.
 - TEXT format, JSON format 두가지로 로그 출력이 가능합니다. TEXT format은 디버딩용, JSON format은 es 전달용으로 사용가능합니다.
 

## supported log type
 - START: application 시작중임을 알려줌
 - FINISH: application 이 종료중임을 알려줌
 - REQEST(http): http request를 수신했음을 알려줌
 - RESPONSE(http): http response 를 송신했음을 알려줌 
 - CONNECT(http): http call 이 발생했음을 알려줌


## 상세 스팩
 - [Tracer core](tracer-core)
 - [Tracer webmvc](tracer-webmvc)
 - [Tracer webflux](tracer-webflux)
 - [Tracer webClient](tracer-webclient)
 - [Tracer task](tracer-task)


# sample project
tracer 를 사용해서 로그 출력하는 프로젝트 샘플입니다.
https://github.com/adrenalinee/tracer-sample


# Quick Start

## spring boot 프로젝트 생성
Spring initializr나 IDE를 통해서 spring-boot 프로젝트를 생성합니다. 생성된 프로젝트의 build.gradle.kts 파일을 열어서 아래와 같이 추가합니다.
spring-webflux 기반 샘플입니다.

```kotlin
repositories {
    mavenCentral()
    maven {
        setUrl("http://kicc-nexus-repo.kep.k9d.in/repository/maven-public")
        isAllowInsecureProtocol = true
    }
}

dependencies {
    implementation("malibu.tracer:tracer-webflux:13.0")
}
```

## tracer 설정
아래와 같이 `TracerWebfluxConfiguration` 을 spring application에 import 합니다.

```kotlin
import malibu.tracer.webflux.TracerWebfluxConfiguration

@Import(TracerWebfluxConfiguration::class)
@SpringBootApplication
class DemoTracerWebfluxApplication
```

## request handler 작성

```kotlin
@RestController
class HelloRestController {
    @GetMapping("/hello")
    fun hello(): Mono<String> {
        return Mono.just("hello world!")
    }
}
```

## application.properties 파일 설정.
src/main/kotlin/resources 폴더에 application.properties 파일을 생성하고 아래와 같이 입력합니다.
TEXT 포맷 로그를 출력합니다.

```
spring.main.banner-mode=off
logging.level.root=WARN
logging.level.TRACER_LOG=INFO
```

## server start
```shell
./gradlew bootRun
```

## http request
```shell
curl http://localhost:8080/hello
```


## log message 확인
```
2022-07-12 16:20:49.283  INFO 19773 --- [           main] TRACER_LOG : (ae4f596b-a) START: application starting...
2022-07-12 16:22:37.597  INFO 19773 --- [ctor-http-nio-3] TRACER_LOG : [d03b47bf-1] REQUEST: HTTP request: GET http://localhost:8080/hello
2022-07-12 16:22:37.681  INFO 19773 --- [ctor-http-nio-3] TRACER_LOG : [d03b47bf-1] RESPONSE: HTTP response: 200 OK
```




# Appendix

## custom field 샘플
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


## custom log type 샘플 (spring-webmvc)
```kotlin
data class CustomTypeTraceLog(
    val data1: String
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
            message = "console 용 메시지 입니다." //TEXT 포맷 로그에 출력될 문자
        )
    }
}

@Component
class UserService(
    private val customTraceLogger: CustomTraceLogger
) {
    fun someAction() {
        customTraceLogger.info( //CUSTOM_LOG_TYPE 타입 로그 출력
            customTypeTraceLog = CustomTypeTraceLog(
                data1 = "data1"
            )
        )
    }
}
```



## logback 설정 샘플

### TEXT format log 출력할 경우 logback-spring.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/base.xml"/>
</configuration>
```


### JSON format, TRACER_CONSOLE appender 사용할 경우: logback-spring.xml
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

### JSON format, TRACER_FILE appender 사용할 경우: logback-spring.xml
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

### JSON format, TRACER_TCP appender 사용할 경우: logback-spring.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="malibu/tracer/logback/tracer-tcp-appender.xml" />

    <logger name="TRACER_LOG">
        <level value="INFO" />
        <appender-ref ref="TRACER_TCP" />
    </logger>
</configuration>
```



## filebeat 설정 샘플

### TRACER_FILE appender 사용할 경우: filebeat.yml
```yaml
filebeat.inputs:
  - type: log
    enabled: true
    fields_under_root: true
    paths:
      - logs/tracer.log
processors:
  - decode_json_fields:
      fields: ["message"]
      max_depth: 1
      target: ""
      overwrite_keys: true
  - drop_fields:
      fields:
        - message
output.elasticsearch:
  hosts: "http://localhost:9200"
setup.kibana:
  host: "http://localhost:5601"
```

### TRACER_TCP appender 사용할 경우: filebeat.yml
```yaml
filebeat.inputs:
  - type: tcp
    enabled: true
    host: "localhost:10000"
    fields_under_root: true
processors:
  - decode_json_fields:
      fields: ["message"]
      max_depth: 1
      target: ""
      overwrite_keys: true
  - drop_fields:
      fields:
        - message
output.elasticsearch:
  hosts: "http://localhost:9200"
setup.kibana:
  host: "http://localhost:5601"
```

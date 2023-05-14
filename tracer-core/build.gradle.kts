dependencies {
    api("org.springframework:spring-core")
    api("org.springframework:spring-context")
    api("com.fasterxml.jackson.core:jackson-databind")

//    api("javax.annotation:javax.annotation-api:1.3.2")
    api("jakarta.annotation:jakarta.annotation-api")
    api("net.logstash.logback:logstash-logback-encoder:7.3")
    api("io.github.microutils:kotlin-logging:3.0.5")


    compileOnly("io.projectreactor:reactor-core")
}
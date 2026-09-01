dependencies {
    api("org.springframework:spring-core")
    api("org.springframework:spring-context")

    api("jakarta.annotation:jakarta.annotation-api")
    api("net.logstash.logback:logstash-logback-encoder:9.0")
    api("io.github.microutils:kotlin-logging:3.0.5")

    implementation("tools.jackson.core:jackson-databind:3.1.0")

    compileOnly("com.google.cloud:spring-cloud-gcp-logging:8.0.1")
    compileOnly("io.projectreactor:reactor-core")
}
dependencies {
    api("org.springframework:spring-core")
    api("org.springframework:spring-context")

    api("jakarta.annotation:jakarta.annotation-api")
    api("net.logstash.logback:logstash-logback-encoder:9.0")
    api("io.github.microutils:kotlin-logging:3.0.5")

//    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("tools.jackson.core:jackson-databind")
    implementation("com.google.cloud:spring-cloud-gcp-logging")

    compileOnly("io.projectreactor:reactor-core")
}
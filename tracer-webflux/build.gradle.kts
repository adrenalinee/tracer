//version = "0.0.3-SNAPSHOT"

dependencies {
    api(project(":tracer-core"))
    api("io.projectreactor:reactor-core")
    api("org.springframework:spring-webflux")

    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

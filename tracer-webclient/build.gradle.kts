//version = "0.0.3-SNAPSHOT"

dependencies {
    api(project(":tracer-core"))
    api("org.springframework:spring-webflux")

    compileOnly(project(":tracer-webmvc"))

    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

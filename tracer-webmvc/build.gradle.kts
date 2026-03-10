dependencies {
    api(project(":tracer-core"))
    api("org.reactivestreams:reactive-streams")
    api("org.springframework:spring-webmvc")
    api("jakarta.servlet:jakarta.servlet-api")

    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-resttestclient")
    testImplementation("org.springframework.boot:spring-boot-restclient")
}

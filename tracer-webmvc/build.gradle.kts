//version = "0.0.3-SNAPSHOT"

dependencies {
    api(project(":tracer-core"))
    api("org.springframework:spring-webmvc")
//    api("javax.servlet:javax.servlet-api")
    api("jakarta.servlet:jakarta.servlet-api")

//    testImplementation("org.springframework:spring-test")
//    testImplementation("org.springframework:spring-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
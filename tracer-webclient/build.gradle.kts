//version = "0.0.3-SNAPSHOT"

dependencies {
    api(project(":tracer-core"))
    api("org.springframework:spring-webflux")

    compileOnly(project(":tracer-webmvc"))
}
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-library") //apply(false)
//    id("maven-publish") //apply(false)
    `maven-publish`

    id("io.spring.dependency-management") version("1.1.7") //apply(false)

    kotlin("jvm") version("1.9.25") apply(false)
}

//allprojects {
//    repositories {
//        mavenCentral()
//    }
//}

tasks.named<Jar>("jar") {
    enabled = false
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "io.spring.dependency-management")

    group = "malibu.tracer"
    version = "14.0-SNAPSHOT"

//    java.sourceCompatibility = JavaVersion.VERSION_17
    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

        testImplementation("org.junit.jupiter:junit-jupiter-engine")
        testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
        testImplementation("org.mockito:mockito-inline:5.2.0")
    }

    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.6")
        }

        imports {
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.0.0")
        }
    }

    java {
        withSourcesJar()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "21"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks["publish"].dependsOn(tasks["build"])
    tasks["publishToMavenLocal"].dependsOn(tasks["build"])

    configure<PublishingExtension> {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/adrenalinee/tracer")
                credentials {
                    username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                    password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
                }
//                val releasesRepoUrl = uri("")
//                val snapshotsRepoUrl = uri("")
//                url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
//                isAllowInsecureProtocol = true
            }
        }

        publications {
            register<MavenPublication>("gpr") {
                from(components["java"])
            }
//            create<MavenPublication>("maven") {
//                groupId = group.toString()
//                artifactId = project.name
//                version = version
//
//
//                from(components["java"])
//            }
        }
    }
}
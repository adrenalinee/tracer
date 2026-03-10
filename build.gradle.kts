import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-library") //apply(false)
//    id("maven-publish") //apply(false)
    `maven-publish`

    id("io.spring.dependency-management") version("1.1.7") //apply(false)

    kotlin("jvm") version("2.2.21") apply(false)
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
    version = "14.0"

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

//        testImplementation("org.junit.jupiter:junit-jupiter-engine")
        testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
//        testImplementation("org.mockito:mockito-inline:5.2.0")
        testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.3")
        }

        imports {
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.1.1")
        }
    }

    java {
        withSourcesJar()
    }

//    tasks.withType<KotlinCompile> {
//        kotlinOptions {
//            freeCompilerArgs = listOf("-Xjsr305=strict")
//            jvmTarget = "21"
//        }
//    }

//    kotlin {
//        compilerOptions {
//            freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
//        }
//    }

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
                    username = System.getenv("USERNAME")?: project.findProperty("gpr.user") as String?
                    password = System.getenv("TOKEN")?: project.findProperty("gpr.key") as String?
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
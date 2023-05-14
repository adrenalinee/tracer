import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-library") //apply(false)
    id("maven-publish") //apply(false)

    id("io.spring.dependency-management") version("1.1.0") //apply(false)

    kotlin("jvm") version("1.7.22") apply(false)
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
    version = "13.0"

    java.sourceCompatibility = JavaVersion.VERSION_17

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

        testImplementation("org.junit.jupiter:junit-jupiter-engine")
        testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
        testImplementation("org.mockito:mockito-inline")
    }

    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:3.0.5")
        }

        imports {
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:2022.0.2")
        }
    }

    java {
        withSourcesJar()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "17"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks["publish"].dependsOn(tasks["build"])
    tasks["publishToMavenLocal"].dependsOn(tasks["build"])

    publishing {
        repositories {
            maven {
                credentials {
                    username = System.getProperty("username")
                    password = System.getProperty("password")
                }
                val releasesRepoUrl = uri("")
                val snapshotsRepoUrl = uri("")
                url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
                isAllowInsecureProtocol = true
            }
        }

        publications {
            create<MavenPublication>("maven") {
                groupId = group.toString()
                artifactId = project.name
                version = version


                from(components["java"])
            }
        }
    }
}
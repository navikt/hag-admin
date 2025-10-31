import kotlin.collections.forEach

val kotlin_version: String by project
val ktor_version: String by project
val logback_version: String by project
val mockk_version: String by project
val arbeidsgiver_notifikasjon_klient_version: String by project

plugins {
    application
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    id("org.jmailen.kotlinter") version "5.2.0"
}

group = "no.nav.hag"
version = "0.0.1"

application {
    mainClass.set("no.nav.hag.ApplicationKt")
}

repositories {
    val githubPassword: String by project
    mavenCentral()
    maven {
        setUrl("https://maven.pkg.github.com/navikt/*")
        credentials {
            username = "x-access-token"
            password = githubPassword
        }
    }
}

tasks {
    named<Jar>("jar") {
        val dependencies = configurations.runtimeClasspath.get()
        manifest {
            attributes["Main-Class"] = "no.nav.hag.ApplicationKt"
            attributes["Class-Path"] = dependencies.joinToString(separator = " ") { it.name }
        }

        doLast {
            dependencies.forEach {
                val file = layout.buildDirectory.file("libs/${it.name}").get().asFile
                if (!file.exists()) {
                    it.copyTo(file)
                }
            }
        }
    }
}
dependencies {
    implementation ("net.logstash.logback:logstash-logback-encoder:8.0")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-client-apache5:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-html-builder:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-metrics-micrometer:${ktor_version}")
    implementation("io.micrometer:micrometer-registry-prometheus:1.10.3")
    implementation("no.nav.helsearbeidsgiver:arbeidsgiver-notifikasjon-klient:$arbeidsgiver_notifikasjon_klient_version")
    implementation("no.nav.helsearbeidsgiver:utils:0.10.1")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-css:1.0.0-pre.817")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    testImplementation("io.ktor:ktor-client-core:$ktor_version")
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("io.mockk:mockk:$mockk_version")
}

val kotlin_version: String by project
val logback_version: String by project
val mockk_version: String by project
val arbeidsgiver_notifikasjon_klient_version: String by project

plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    id("io.ktor.plugin") version "3.1.2"
    id("org.jmailen.kotlinter") version "5.2.0"
}

group = "no.nav.hag"
version = "0.0.1"

application {
    mainClass.set("no.nav.hag.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
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

dependencies {
    implementation ("net.logstash.logback:logstash-logback-encoder:8.0")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-client-apache5")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-client-core")
//    implementation("io.ktor:ktor-client-plugins")
//    implementation("io.ktor:ktor-client-logging")
//    implementation("io.ktor:ktor-client-auth")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-auth-jvm")
    implementation("io.ktor:ktor-server-auth-jwt-jvm")
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-server-html-builder")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("no.nav.helsearbeidsgiver:arbeidsgiver-notifikasjon-klient:$arbeidsgiver_notifikasjon_klient_version")
    implementation("no.nav.helsearbeidsgiver:utils:0.9.0")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-css:1.0.0-pre.817")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    testImplementation("io.ktor:ktor-client-core")
//    testImplementation("io.ktor:ktor-client-plugins")
    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("io.mockk:mockk:$mockk_version")
}

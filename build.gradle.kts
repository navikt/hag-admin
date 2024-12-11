val kotlin_version: String by project
val logback_version: String by project
val mockk_version: String by project

plugins {
    kotlin("jvm") version "2.0.20"
    id("io.ktor.plugin") version "2.3.12"
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
    implementation("no.nav.helsearbeidsgiver:arbeidsgiver-notifikasjon-klient:2.7.0")
    implementation("no.nav.helsearbeidsgiver:tokenprovider:0.4.0")
    implementation("no.nav.security:token-validation-ktor-v2:5.0.5")
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-auth-jvm")
    implementation("io.ktor:ktor-server-auth-jwt-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-html-builder")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-css:1.0.0-pre.817")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation ("net.logstash.logback:logstash-logback-encoder:8.0")
    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("no.nav.security:mock-oauth2-server:2.1.9")
    testImplementation("io.mockk:mockk:$mockk_version")
}

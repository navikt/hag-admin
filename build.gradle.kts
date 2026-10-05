import kotlin.collections.forEach
val kotlinVersion = project.property("kotlinVersion") as String
val ktorVersion = project.property("ktorVersion") as String
val kafkaClientVersion = project.property("kafkaClientVersion") as String
val logbackVersion = project.property("logbackVersion") as String
val mockkVersion = project.property("mockkVersion") as String
val arbeidsgiverNotifikasjonKlientVersion = project.property("arbeidsgiverNotifikasjonKlientVersion") as String
val brregKlientVersion = project.property("brregKlientVersion") as String
val utilsVersion = project.property("utilsVersion") as String


plugins {
    application
    kotlin("jvm") version "2.4.0"
    kotlin("plugin.serialization") version "2.4.0"
    id("org.jmailen.kotlinter") version "5.7.0"
}

group = "no.nav.hag"
version = "0.0.1"

kotlin {
    jvmToolchain(25)
}

application {
    mainClass.set("no.nav.hag.ApplicationKt")
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "io.netty") {
            useVersion("4.2.18.Final")
            because("Override Ktor's Netty til en nyere versjon")
        }
    }
}

repositories {
    val githubPassword = project.property("githubPassword") as String
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
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.ktor:ktor-client-apache5:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-html-builder:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:1.10.3")
    implementation("no.nav.helsearbeidsgiver:arbeidsgiver-notifikasjon-klient:$arbeidsgiverNotifikasjonKlientVersion")
    implementation("no.nav.helsearbeidsgiver:brreg-client:$brregKlientVersion")
    implementation("no.nav.helsearbeidsgiver:utils:$utilsVersion")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-css:1.0.0-pre.817")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("org.apache.kafka:kafka-clients:$kafkaClientVersion")
    testImplementation(testFixtures("no.nav.helsearbeidsgiver:utils:$utilsVersion"))
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
}

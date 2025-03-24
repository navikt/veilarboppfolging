val kotlinVersion = "2.0.20"
val springBootVersion = "3.3.4"
val dependencyManagementVersion = "1.1.3"
val jacocoVersion = "0.8.12"
val flywayVersion = "10.15.2"
val commonVersion = "3.2025.03.06_11.40-cbc2a0783de9"
val ptoSchemaVersion = "1.2025.01.13_12.58-3e81bd940198"
val poaoTilgangVersion = "2025.02.06_13.37-958e35e7373d"
val wiremockVersion = "3.0.0-beta-10"
val schedlockVersion = "6.3.0"
val googleCloudBigQueryVersion = "2.43.2"
val googleCloudLibrariesBomVersion = "26.49.0"
val springDoc = "2.8.5"
val tmsMicrofrontendBuilder = "3.0.0"
val tmsVarselBuilder = "2.1.1"
val avroVersion = "1.12.0"
val confluentKafkaAvroVersion = "7.9.0"

plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.spring") version "2.1.0"
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.3"
    id("jacoco")
    id("org.sonarqube") version "6.0.1.5171"
    kotlin("plugin.lombok") version "2.1.20"
}

group = "no.nav"
java.sourceCompatibility = JavaVersion.VERSION_21

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
//    reports {
//        xml.required.set(true)
//    }
}

tasks.sonar {
    dependsOn(tasks.jacocoTestReport)
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
    maven {
        url = uri("https://repo.jenkins-ci.org/public/")
    }
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    annotationProcessor("org.projectlombok:lombok:1.18.36")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.36")

    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-graphql")
    implementation("org.springframework.boot:spring-boot-starter-logging")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("com.squareup.okhttp3:okhttp")
    implementation("io.micrometer:micrometer-registry-prometheus-simpleclient")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDoc")
    implementation("org.springframework.boot:spring-boot-devtools")
    implementation("org.projectlombok:lombok:1.18.32")
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    implementation("no.nav.poao-tilgang:client:$poaoTilgangVersion")
    implementation("com.zaxxer:HikariCP")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")
    implementation("org.postgresql:postgresql")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:$schedlockVersion")
    implementation("no.nav:pto-schema:$ptoSchemaVersion")
    implementation("no.nav.common:job:$commonVersion")
    implementation("no.nav.common:util:$commonVersion")
    implementation("no.nav.common:types:$commonVersion")
    implementation("no.nav.common:client:$commonVersion")
    implementation("no.nav.common:auth:$commonVersion")
    implementation("no.nav.common:audit-log:$commonVersion")
    implementation("no.nav.common:token-client:$commonVersion")
    implementation("no.nav.common:log:$commonVersion")
    implementation("no.nav.common:health:$commonVersion")
    implementation("no.nav.common:metrics:$commonVersion")
    implementation("no.nav.common:kafka:$commonVersion")
    implementation("org.apache.avro:avro:$avroVersion")
    implementation("io.confluent:kafka-avro-serializer:$confluentKafkaAvroVersion") {
        exclude(group = "io.swagger.core.v3")
    }
    implementation("no.nav.tms.mikrofrontend.selector:builder:$tmsMicrofrontendBuilder")
    implementation("no.nav.tms.varsel:kotlin-builder:$tmsVarselBuilder")
    implementation("com.google.cloud:google-cloud-bigquery:$googleCloudBigQueryVersion")
    implementation(platform("com.google.cloud:libraries-bom:$googleCloudLibrariesBomVersion"))

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("no.nav.common:test:$commonVersion")
    testImplementation("org.springframework.graphql:spring-graphql-test:1.3.3")
    testImplementation("io.zonky.test:embedded-database-spring-test:2.5.1")
    testImplementation("io.zonky.test:embedded-postgres:2.0.7")
    testImplementation("junit:junit")
    testImplementation("org.junit.vintage:junit-vintage-engine")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("com.github.tomakehurst:wiremock-standalone:$wiremockVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "com.vaadin.external.google", module = "android-json")
    }
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jacoco {
    toolVersion = jacocoVersion
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

sonarqube {
    properties {
        property("sonar.projectKey", "navikt_veilarboppfolging")
        property("sonar.organization", "navikt")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.java.binaries", "${project.layout.buildDirectory.get().asFile}/classes/java/main")
    }
}

tasks.withType<Test>().configureEach {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}

if (hasProperty("buildScan")) {
    extensions.findByName("buildScan")?.withGroovyBuilder {
        setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
        setProperty("termsOfServiceAgree", "yes")
    }
}

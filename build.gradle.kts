val kotlinVersion = "2.3.21"
val flywayVersion = "12.0.3"
val commonVersion = "4.2026.04.29_05.55-9f2b107283bc"
val ptoSchemaVersion = "1.2025.09.29_11.36-6e568fa24c23"
val poaoTilgangVersion = "2025.07.04_08.56-814fa50f6740"
val wiremockVersion = "3.13.2"
val schedlockVersion = "7.7.0"
val googleCloudLibrariesBomVersion = "26.80.0"
val springDoc = "3.0.3"
val tmsMicrofrontendBuilder = "3.0.0"
val tmsVarselBuilder = "2.2.0"
val logstashVersion = "9.0"
val avroVersion = "1.12.1"
val confluentKafkaAvroVersion = "8.2.0"
val okHttpVersion = "5.3.2"
val dabBigQuerySchemaVersion = "2026.04.20-16.33.4a120cb625c2"

plugins {
    val kotlinVersion = "2.3.21"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.lombok") version kotlinVersion
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("jacoco")
    id("org.sonarqube") version "7.2.3.7755"
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
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
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
}

dependencies {
    annotationProcessor("org.projectlombok:lombok:1.18.46")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.46")

    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-graphql")
    implementation("org.springframework.boot:spring-boot-starter-logging")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("com.squareup.okhttp3:okhttp-jvm:$okHttpVersion")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-observation")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDoc")
    implementation("org.springframework.boot:spring-boot-devtools")
    implementation("org.projectlombok:lombok:1.18.46")
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
    implementation(platform("com.google.cloud:libraries-bom:$googleCloudLibrariesBomVersion"))
    implementation("com.google.cloud:google-cloud-bigquery")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")
    implementation("no.nav.poao.dab:bigquery-schema:${dabBigQuerySchemaVersion}")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("no.nav.common:test:$commonVersion")
    testImplementation("org.springframework.graphql:spring-graphql-test")
    testImplementation("io.zonky.test:embedded-database-spring-test:2.8.0")
    testImplementation("io.zonky.test:embedded-postgres:2.2.2")
    testImplementation("junit:junit")
    testImplementation("org.junit.vintage:junit-vintage-engine")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "com.vaadin.external.google", module = "android-json")
    }
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    testImplementation("no.nav.poao.dab:bigquery-schema:$dabBigQuerySchemaVersion:test-fixtures")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.3.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
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

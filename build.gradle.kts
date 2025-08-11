plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.serialization)
    application
    alias(libs.plugins.git.versioning)
    alias(libs.plugins.gradle.git.properties)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    id("maven-publish")
    jacoco
    alias(libs.plugins.ktlint)
    alias(libs.plugins.dependencyCheck)
}

group = "io.github.alkoleft.mcp"
version = "1.0-SNAPSHOT"

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-java-parameters", "-Xemit-jvm-type-annotations")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    // Kotlin Standard Library
    implementation(libs.bundles.kotlin)

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.8.0")

    // Spring Boot
    implementation(libs.bundles.spring.boot)

    // JSON/XML with Kotlin support
    implementation(libs.bundles.jackson)

    // Logging
    implementation(libs.bundles.logging)

    // Reactor Core для Spring AI MCP
    implementation(libs.reactor.core)

    // MapDB for persistent storage
    implementation("org.mapdb:mapdb:3.0.10")

    // Tests
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.bundles.junit)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.assertj.core)
    testImplementation(libs.slf4j.log4j12)
    testImplementation("io.mockk:mockk:1.14.5")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${libs.versions.springAi.get()}")
    }
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed", "standard_error")
    }
}

tasks.jar {
    enabled = false
    archiveClassifier.set("plain")
}

tasks.bootJar {
    enabled = true
    archiveClassifier.set("")
    mainClass.set("io.github.alkoleft.mcp.MainKt")
}

// Исправление зависимостей для задач распространения
tasks.named("bootDistZip") {
    dependsOn("bootJar")
}

tasks.named("bootDistTar") {
    dependsOn("bootJar")
}

tasks.named("bootStartScripts") {
    dependsOn("bootJar")
}

tasks.named("startScripts") {
    dependsOn("bootJar")
}

publishing {
    repositories {
        maven {
            name = "mcp-yaxunit-runner"
            url = uri("https://maven.pkg.github.com/alkoleft/mcp-yaxunit-runner")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
        }
    }
}

// Настройка JaCoCo для генерации отчёта покрытия тестов
jacoco {
    toolVersion = libs.versions.jacoco.get()
}

ktlint {
    version = libs.versions.ktlint.get()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

publishing {
    repositories {
        maven {
            name = "mcp-onec-test-runner"
            url = uri("https://maven.pkg.github.com/alkoleft/mcp-onec-test-runner")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
        }
    }
}


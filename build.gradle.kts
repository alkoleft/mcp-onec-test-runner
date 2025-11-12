/*
 * This file is part of METR.
 *
 * Copyright (C) 2025 Aleksey Koryakin <alkoleft@gmail.com> and contributors.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * METR is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * METR is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with METR.  If not, see <https://www.gnu.org/licenses/>.
 */

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
version = "0.4.0-SNAPSHOT"

gitVersioning.apply {
    refs {
        considerTagsOnBranches = true
        tag("v(?<tagVersion>[0-9].*)") {
            version = "\${ref.tagVersion}\${dirty}"
        }
        branch(".+") {
            version = "\${ref}-\${commit.short}\${dirty}"
        }
    }

    rev {
        version = "\${commit.short}\${dirty}"
    }
}

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
            name = "mcp-onec-test-runner"
            url = uri("https://maven.pkg.github.com/alkoleft/mcp-onec-test-runner")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        create<MavenPublication>("gpr") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            artifact(tasks.named("bootJar"))
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

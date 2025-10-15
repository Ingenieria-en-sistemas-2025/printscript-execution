plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("jacoco")
    id("com.diffplug.spotless") version "6.25.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
}

group = "com.printscript"
version = "0.0.1-SNAPSHOT"
description = "PrintScript Execution microservice"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    // mavenLocal()
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/Ingenieria-en-sistemas-2025/PrintScriptV1")
        credentials {
            username = findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
            password = findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
        }
    }
}


val psver = "1.0.9";

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.printscript:runner:${psver}")
    implementation("org.printscript:common:${psver}")
    implementation("org.printscript:token:${psver}")
    implementation("org.printscript:ast:${psver}")
    implementation("org.printscript:lexer:${psver}")
    implementation("org.printscript:parser:${psver}")
    implementation("org.printscript:analyzer:${psver}")
    implementation("org.printscript:formatter:${psver}")
    implementation("org.printscript:interpreter:${psver}")
    implementation("org.printscript:cli:${psver}")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
// tengo que traerme el cli por el version support o agregarlo en otro lado

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

spotless {
    kotlin {
        target("**/*.kt")
        ktlint("0.50.0").editorConfigOverride(
            mapOf("max_line_length" to "400", "indent_size" to "4"),
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        ktlint("0.50.0")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config = files("$rootDir/config/detekt/detekt.yml")
}

jacoco { toolVersion = "0.8.10" }

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

tasks.register<JacocoCoverageVerification>("jacocoVerify") {
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal() // 80%
            }
        }
    }
}

tasks.check { dependsOn("detekt", "spotlessCheck", "jacocoVerify") }

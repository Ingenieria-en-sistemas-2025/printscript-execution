
plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.spring") version "2.2.10"
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("jacoco")
    id("com.diffplug.spotless") version "6.25.0"
    id("dev.detekt") version "2.0.0-alpha.0"
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
    mavenCentral()
    maven {
        name = "PrintscriptV1"
        url = uri("https://maven.pkg.github.com/Ingenieria-en-sistemas-2025/PrintScriptV1")
        credentials {
            username = (findProperty("gpr.user") as String?)
                ?: System.getenv("GITHUB_ACTOR")
            password = (findProperty("gpr.key") as String?)
                ?: System.getenv("GITHUB_TOKEN")
        }
    }

    maven {
        name = "ClassRedisStreams"
        url = uri("https://maven.pkg.github.com/austral-ingsis/class-redis-streams")
        credentials {
            username = (findProperty("gpr.user") as String?) ?: System.getenv("GITHUB_ACTOR")
            password = (findProperty("gpr.key") as String?) ?: System.getenv("GITHUB_TOKEN")
        }
    }

    maven {
        name = "PrintscriptContracts"
        url = uri("https://maven.pkg.github.com/Ingenieria-en-sistemas-2025/printscript-contracts")
        credentials {
            username = (findProperty("gpr.user") as String?) ?: System.getenv("GITHUB_ACTOR")
            password = (findProperty("gpr.key") as String?) ?: System.getenv("GITHUB_TOKEN")
        }
    }
}
val psver = "0.3.2"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.austral.ingsis:redis-streams-mvc:0.1.13")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.printscript:runner:$psver")
    implementation("io.printscript:contracts:0.1.1")
    implementation("com.newrelic.agent.java:newrelic-api:8.10.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.13.11")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict") // trata las anotaciones Java como si fueran nativas de Kotlin
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

spotless {
    kotlin {
        target("**/*.kt")
        ktlint("1.7.1").editorConfigOverride(
            mapOf(
                "max_line_length" to "200",
                "indent_size" to "4",
            ),
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        ktlint("1.7.1")
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

tasks.jacocoTestCoverageVerification {
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

tasks.check {
    dependsOn("detekt", "spotlessCheck", tasks.jacocoTestCoverageVerification)
}

// Git hooks
val gitDir = layout.projectDirectory.dir(".git")
val hooksSrc = layout.projectDirectory.dir("hooks")
val hooksDst = layout.projectDirectory.dir(".git/hooks")

tasks.register<Copy>("installGitHooks") {
    onlyIf { gitDir.asFile.exists() && hooksSrc.asFile.exists() }
    from(hooksSrc)
    into(hooksDst)
    fileMode = Integer.parseInt("775", 8) // chmod +x
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.register("ensureGitHooks") {
    dependsOn("installGitHooks")
    onlyIf { gitDir.asFile.exists() }
}

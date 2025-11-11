import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/Ingenieria-en-sistemas-2025/PrintScriptV1")
        credentials {
            username = (findProperty("gpr.user") as String?)
                ?: System.getenv("GITHUB_ACTOR")
            password = (findProperty("gpr.key") as String?)
                ?: System.getenv("GITHUB_TOKEN")
        }
    }

    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/austral-ingsis/class-redis-streams")
        credentials {
            username = (findProperty("gpr.user") as String?) ?: System.getenv("GITHUB_ACTOR")
            password = (findProperty("gpr.key") as String?) ?: System.getenv("GITHUB_TOKEN")
        }
    }
}
val psver = "1.0.13"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.printscript:runner:$psver")
    implementation("org.printscript:common:$psver")
    implementation("org.printscript:token:$psver")
    implementation("org.printscript:ast:$psver")
    implementation("org.printscript:lexer:$psver")
    implementation("org.printscript:parser:$psver")
    implementation("org.printscript:analyzer:$psver")
    implementation("org.printscript:formatter:$psver")
    implementation("org.printscript:interpreter:$psver")
    implementation("org.printscript:cli:$psver")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:2.2.10"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.austral.ingsis:redis-streams-mvc:0.1.13")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.austral.ingsis:redis-streams-mvc:0.1.13")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

configurations.testRuntimeClasspath {
    exclude(group = "org.austral.ingsis", module = "redis-streams-mvc")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
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
        ktlint("1.7.1").editorConfigOverride(
            mapOf(
                "max_line_length" to "400",
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

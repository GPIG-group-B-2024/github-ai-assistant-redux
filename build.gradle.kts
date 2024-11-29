import org.jooq.meta.jaxb.Logging
import org.testcontainers.containers.PostgreSQLContainer

plugins {
  kotlin("jvm") version "2.0.21"
  id("nu.studer.jooq") version "8.2.1"
  kotlin("plugin.spring") version "2.0.21"
  id("org.flywaydb.flyway") version "9.7.0"
  id("com.diffplug.spotless") version "6.25.0"
  id("org.springframework.boot") version "3.3.5"
  id("io.spring.dependency-management") version "1.1.6"
}

buildscript {
  repositories { mavenCentral() }
  dependencies {
    classpath("org.testcontainers:postgresql:1.17.6")
    classpath("org.flywaydb:flyway-core:9.10.2")
  }
}

group = "uk.ac.york.gpig.teamb"

version = "0.0.1-SNAPSHOT"

java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

repositories { mavenCentral() }

dependencyManagement {
  imports { mavenBom("io.github.sparsick.testcontainers.gitserver:testcontainers-git-bom:0.10.0") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.eclipse.jgit:org.eclipse.jgit:7.0.0.202409031743-r") // git API
  implementation("org.kohsuke:github-api:1.326") // GitHub API
  implementation("com.google.code.gson:gson:2.11.0") // JSON handling
  implementation("com.auth0:java-jwt:4.4.0") // JWT generation
  // jooq (database) =========
  implementation("org.jooq:jooq:3.19.15")
  implementation("org.flywaydb:flyway-core:9.10.2")
  implementation("org.postgresql:postgresql:42.7.2")
  jooqGenerator("org.postgresql:postgresql:42.7.2")
  implementation("org.springframework.boot:spring-boot-starter-jdbc")
  // =========================
  implementation(
      "com.structurizr:structurizr-dsl:3.1.0") // for parsing incoming structurizr content from LLM
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testImplementation("io.strikt:strikt-core:0.35.1") // assertions
  testImplementation("com.ninja-squad:springmockk:4.0.2") // mocking
  testImplementation(
      "io.github.sparsick.testcontainers.gitserver:testcontainers-gitserver") // mock git server
  testImplementation("com.maciejwalkowiak.spring:wiremock-spring-boot:2.1.3")
  testImplementation("org.testcontainers:postgresql:1.17.6")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin { compilerOptions { freeCompilerArgs.addAll("-Xjsr305=strict") } }

tasks.withType<Test> { useJUnitPlatform() }

// formatting

spotless {
  kotlin {
    ktfmt()
    ktlint()
    target("src/**/*.kt")
    toggleOffOn()
  }
  kotlinGradle {
    target("*.gradle.kts") // default target for kotlinGradle
    ktfmt()
  }
}

// Generate Jooq classes and types

// Create a postgres testcontainer
val container =
    if ("generateJooq" in project.gradle.startParameter.taskNames) {
      PostgreSQLContainer("postgres:15.4").apply {
        withDatabaseName("github_ai_assistant")
        start()
      }
    } else null

// apply migrations
flyway {
  logging.captureStandardOutput(LogLevel.INFO)
  url = container?.getJdbcUrl()
  user = container?.username
  password = container?.password
  schemas = arrayOf("github_ai_assistant")
  createSchemas = true
}

// generate classes
jooq {
  configurations {
    create("main") {
      jooqConfiguration.apply {
        logging = Logging.WARN
        jdbc.apply {
          driver = "org.postgresql.Driver"
          url = container?.getJdbcUrl()
          user = container?.username
          password = container?.password
        }
        generator.apply {
          name = "org.jooq.codegen.KotlinGenerator"
          database.apply {
            name = "org.jooq.meta.postgres.PostgresDatabase"
            includes = ".*"
            excludes = ""
            inputSchema = "github_ai_assistant"
          }
          target.apply {
            isClean = true
            packageName = "uk.ac.york.gpig.teamb.aiassistant"
            directory = "build/generated/jooq"
          }
        }
      }
    }
  }
}

tasks.flywayMigrate.configure {
  val taskNames = project.gradle.startParameter.taskNames
  onlyIf { taskNames == listOf("generateJooq") }
}

tasks.named("generateJooq").configure {
  doLast { container?.stop() }
  dependsOn(tasks.named("flywayMigrate"))
  val taskNames = project.gradle.startParameter.taskNames
  onlyIf { taskNames == listOf("generateJooq") }
}

plugins {
  kotlin("jvm") version "2.0.21"
  kotlin("plugin.spring") version "2.0.21"
  id("com.diffplug.spotless") version "6.25.0"
  id("org.springframework.boot") version "3.3.5"
  id("io.spring.dependency-management") version "1.1.6"
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
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testImplementation("io.strikt:strikt-core:0.35.1") // assertions
  testImplementation("com.ninja-squad:springmockk:4.0.2") // mocking
  testImplementation(
      "io.github.sparsick.testcontainers.gitserver:testcontainers-gitserver") // mock git server
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin { compilerOptions { freeCompilerArgs.addAll("-Xjsr305=strict") } }

tasks.withType<Test> { useJUnitPlatform() }

// formatting

spotless {
  kotlin {
    ktfmt()
    ktlint()
  }
  kotlinGradle {
    target("*.gradle.kts") // default target for kotlinGradle
    ktfmt()
  }
}

plugins {
  kotlin("jvm") version "2.0.21" apply false
  kotlin("plugin.spring") version "2.0.21" apply false
  id("com.diffplug.spotless") version "7.0.2"
}

repositories { mavenCentral() }

// formatting
fun resourcesOfType(type: String) = "**/**/main/resources/**/*.$type"

spotless {
  kotlin {
    ktlint("1.5.0")
    target("**/*.kt")
    toggleOffOn()
  }
  kotlinGradle {
    target("**/*.gradle.kts") // default target for kotlinGradle
    ktfmt()
  }
  format("html") {
    target(resourcesOfType("html"))
    prettier().config(mapOf("parser" to "html"))
  }
  format("css") {
    target(resourcesOfType("css"))
    prettier().config(mapOf("parser" to "css"))
  }
}

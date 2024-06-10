import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.diffplug.spotless")
}

tasks.withType<KotlinCompile>().configureEach { compilerOptions { jvmTarget = JvmTarget.JVM_11 } }

// See https://github.com/diffplug/spotless/tree/main/plugin-gradle
spotless {
  // watch for https://github.com/diffplug/spotless/issues/911 to be closed
  ratchetFrom("origin/develop")
  encoding("UTF-8") // all formats will be interpreted as UTF-8
  val formatter = rootProject.layout.projectDirectory.dir(".formatter")

  format("misc") {
    target("*.gradle", "*.md", ".gitignore", "*.txt", "*.csv")

    trimTrailingWhitespace()
    indentWithSpaces(2)
    endWithNewline()
  }
  java {
    // don"t need to set target, it is inferred from java
    // version list:
    // https://github.com/diffplug/spotless/tree/main/lib-extra/src/main/resources/com/diffplug/spotless/extra/eclipse_jdt_formatter
    eclipse("4.21.0").configFile(formatter.file("eclipse-formatter.xml"))
    licenseHeaderFile(formatter.file("license-header"))
  }
  kotlin {
    // by default the target is every ".kt" and ".kts` file in the java sourcesets
    ktfmt("0.39")
    licenseHeaderFile(formatter.file("license-header"))
  }
  kotlinGradle { ktfmt() }
}

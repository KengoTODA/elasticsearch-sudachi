import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("java-library")
  id("org.jetbrains.kotlin.jvm") version "1.8.0"
  id("org.jetbrains.kotlin.plugin.serialization") version "1.8.0"
  id("com.diffplug.spotless") version "6.16.0"
  id("org.sonarqube") version "4.0.0.2929"
  id("org.jetbrains.kotlinx.kover") version "0.7.0"
  id("com.worksap.nlp.sudachi.esc")
  id("com.worksap.nlp.sudachi.es")
  id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
}

group = "com.worksap.nlp"

val archivesBaseName = "analysis-sudachi"

version = properties["pluginVersion"] ?: "SNAPSHOT"

tasks.withType<KotlinCompile>().configureEach { compilerOptions { jvmTarget = JvmTarget.JVM_11 } }

val spi by configurations.creating {}

sourceSets {
  test {
    compileClasspath += spi
    runtimeClasspath += spi
  }
  main { compileClasspath += spi }
}

dependencies {
  spi(project(":spi"))
  testImplementation(project(":testlib"))
  testImplementation("org.apache.logging.log4j:log4j-core:2.17.2")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit") { exclude(group = "org.hamcrest") }
  testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
  kover(project(":integration"))
  kover(project(":testlib"))
}

val embedVersion =
    tasks.register<Copy>("embedVersion") {
      val esKind = sudachiEs.kind.get()
      from("src/main/extras/plugin-descriptor.properties")
      into("build/package/$version/${esKind.engine.kind}-${esKind.version}")
      expand(
          "version" to version,
          "engineVersion" to esKind.version,
          "engineKind" to esKind.engine.kind)
      inputs.property("version", version)
      inputs.property("elasticSearchVersion", esKind.version)
    }

val packageJars =
    tasks.register<Copy>("packageJars") {
      from(configurations.runtimeClasspath)
      from(tasks.jar.map { it.outputs })
      val esKind = sudachiEs.kind.get()
      into("build/package/$version/${esKind.engine.kind}-${esKind.version}")
      dependsOn(tasks.jar)
    }

val packageSpiJars =
    tasks.register<Copy>("packageSpiJars") {
      from(spi)
      val esKind = sudachiEs.kind.get()
      if (sudachiEs.hasPluginSpiSupport()) {
        into("build/package/$version/${esKind.engine.kind}-${esKind.version}/spi")
      } else {
        into("build/package/$version/${esKind.engine.kind}-${esKind.version}")
      }
    }

val distZip =
    tasks.register<Zip>("distZip") {
      val esKind = sudachiEs.kind.get()
      dependsOn(embedVersion, packageJars, packageSpiJars)
      archiveBaseName.set("${esKind.engine.kind}-${esKind.version}-$archivesBaseName")
      from(
          "build/package/${version}/${esKind.engine.kind}-${esKind.version}",
          "LICENSE",
          "README.md")
    }

artifacts { archives(distZip) }

koverReport {
  defaults {
    xml { setReportFile(layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml")) }
  }
}

// See https://github.com/diffplug/spotless/tree/main/plugin-gradle
spotless {
  // watch for https://github.com/diffplug/spotless/issues/911 to be closed
  ratchetFrom("origin/develop")
  encoding("UTF-8") // all formats will be interpreted as UTF-8

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
    eclipse("4.21.0").configFile(".formatter/eclipse-formatter.xml")
    licenseHeaderFile(".formatter/license-header")
  }
  kotlin {
    // by default the target is every ".kt" and ".kts` file in the java sourcesets
    ktfmt("0.39")
    licenseHeaderFile(".formatter/license-header")
  }
  kotlinGradle { ktfmt() }
}

sonarqube {
  properties {
    property("sonar.projectKey", "WorksApplications_elasticsearch-sudachi")
    property("sonar.organization", "worksapplications")
    property("sonar.host.url", "https://sonarcloud.io")
  }
}

tasks.register("printVersionForGithubActions") {
  doLast {
    // https://docs.github.com/en/actions/using-workflows/workflow-commands-for-github-actions#environment-files
    val esKind = sudachiEs.kind.get()

    file(System.getenv("GITHUB_ENV"))
        .writeText(
            "PROJ_VERSION=${project.version}\nENGINE_VERSION=${esKind.version}\nENGINE_KIND=${esKind.engine.kind}")
  }
}

nexusPublishing {
  repositories {
    sonatype {
      username.set(
          project.findProperty("maven.user")?.toString() ?: System.getenv("MAVEN_USERNAME"))
      password.set(
          project.findProperty("maven.password")?.toString()
              ?: System.getenv("MAVEN_USER_PASSWORD"))
    }
  }
}

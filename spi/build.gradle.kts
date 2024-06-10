plugins {
  id("java-library")
  id("com.diffplug.spotless")
  id("signing")
  id("maven-publish")
  id("com.worksap.nlp.sudachi.es")
  id("com.worksap.nlp.sudachi.esc")
}

group = "com.worksap.nlp"

version = properties["pluginVersion"] ?: "SNAPSHOT"

description =
    "Plugin interface for Sudachi search engine integrations (ElasticSearch and OpenSearch)"

dependencies { api("com.worksap.nlp:sudachi:0.7.3") }

spotless {
  // watch for https://github.com/diffplug/spotless/issues/911 to be closed
  ratchetFrom("origin/develop")
  encoding("UTF-8") // all formats will be interpreted as UTF-8
  val formatter = rootProject.projectDir.toPath().resolve(".formatter")

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

    eclipse("4.21.0").configFile(formatter.resolve("eclipse-formatter.xml"))
    licenseHeaderFile(formatter.resolve("license-header"))
  }
  kotlinGradle { ktfmt() }
}

java {
  withJavadocJar()
  withSourcesJar()
}

tasks.withType<org.gradle.jvm.tasks.Jar>().configureEach {
  archiveBaseName.set("sudachi-search-spi")
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["java"])

      pom {
        artifactId = "sudachi-search-spi"
        url = "https://github.com/WorksApplications/elasticsearch-sudachi"
        name = "sudachi-search-spi"
        description = project.description

        licenses {
          license {
            name = "The Apache License, Version 2.0"
            url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
          }
        }

        developers {
          developer {
            id = "kazuma-t"
            name = "Kazuma TAKAOKA"
            email = "takaoka_k@worksap.co.jp"
            timezone = "Asia/Tokyo"
          }
        }

        scm {
          connection = "scm:git:https://github.com/WorksApplications/elasticsearch-sudachi.git"
          developerConnection =
              "scm:git:ssh://git@github.com:WorksApplications/elasticsearch-sudachi.git"
          url = "https://github.com/WorksApplications/elasticsearch-sudachi"
        }

        issueManagement {
          system = "Github Issues"
          url = "https://github.com/WorksApplications/elasticsearch-sudachi/issues"
        }
      }
    }
  }
}

signing {
  val signingKey =
      project.findProperty("gpg.key")?.toString() ?: System.getenv("MAVEN_GPG_PRIVATE_KEY")
  val signingPassword =
      project.findProperty("gpg.password")?.toString() ?: System.getenv("MAVEN_GPG_PASSPHRASE")
  useInMemoryPgpKeys(signingKey, signingPassword)
  setRequired({ !(project.version as String).endsWith("-SNAPSHOT") })
  sign(publishing.publications["maven"])
}

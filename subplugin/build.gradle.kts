plugins {
  id("java-library")
  id("com.diffplug.spotless")
  id("com.worksap.nlp.sudachi.esc")
  id("com.worksap.nlp.sudachi.es")
}

group = "com.worksap.nlp"

version = properties["pluginVersion"] ?: "SNAPSHOT"

dependencies { compileOnly(project(":spi")) }

val embedVersion =
    tasks.register<Copy>("embedVersion") {
      val esKind = sudachiEs.kind.get()
      from("src/main/extras/plugin-descriptor.properties")
      into("build/package/${version}/${esKind.engine.kind}-${esKind.version}")
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
      into("build/package/${version}/${esKind.engine.kind}-${esKind.version}")
      dependsOn(tasks.jar)
    }

val distZip =
    tasks.register<Zip>("distZip") {
      val esKind = sudachiEs.kind.get()
      dependsOn(embedVersion, packageJars)
      archiveBaseName.set("${esKind.engine.kind}-${esKind.version}-subplugin")
      from("build/package/${version}/${esKind.engine.kind}-${esKind.version}")
    }

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

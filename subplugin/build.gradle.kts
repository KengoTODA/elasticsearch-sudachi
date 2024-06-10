plugins {
  id("java-library")
  id("conventions")
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
      from(tasks.jar)
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

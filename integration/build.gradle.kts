import de.undercouch.gradle.tasks.download.Download

plugins {
  id("java-library")
  id("conventions")
  id("com.worksap.nlp.sudachi.es")
  id("com.worksap.nlp.sudachi.esc")
  id("com.worksap.nlp.sudachi.es.testenv")
  id("org.jetbrains.kotlin.jvm")
  id("de.undercouch.download") version "5.4.0"
}

version = properties["pluginVersion"] ?: "SNAPSHOT"

val buildSudachiDict by configurations.creating {}

dependencies {
  buildSudachiDict(project(":spi"))
  compileOnly(project(":"))
  compileOnly(project(":spi"))
  testCompileOnly(project(":testlib"))
  testCompileOnly(project(":subplugin"))
  testImplementation("junit:junit:4.13.1") { isTransitive = false }
  testImplementation("org.apache.logging.log4j:log4j-core:2.17.2")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit") { isTransitive = false }
}

val compileSystemDictionary =
    tasks.register<JavaExec>("compileTestDictionary") {
      classpath = buildSudachiDict
      mainClass = "com.worksap.nlp.sudachi.dictionary.DictionaryBuilder"
      defaultCharacterEncoding = "utf-8"
      val dictRoot = rootProject.file("src/test/resources/dict").toPath()
      val matrixFile = dictRoot.resolve("matrix.def")
      val dataFile = dictRoot.resolve("lex.csv")
      val resultFile = layout.buildDirectory.file("generated/dict/system.dict")
      args("-d", "test dictionary", "-m", matrixFile, "-o", resultFile, dataFile)
      inputs.file(matrixFile)
      inputs.file(dataFile)
      outputs.file(resultFile)
    }

val downloadIcuPlugin =
    tasks.register<Download>("downloadIcuPlugin") {
      val version = sudachiEs.version()
      if (sudachiEs.isEs()) {
        src(
            "https://artifacts.elastic.co/downloads/elasticsearch-plugins/analysis-icu/analysis-icu-${version}.zip")
        dest(layout.buildDirectory.file("cache/analysis-icu-elasticsearch-${version}.zip"))
      } else {
        src(
            "https://artifacts.opensearch.org/releases/plugins/analysis-icu/${version}/analysis-icu-${version}.zip")
        dest(layout.buildDirectory.file("cache/analysis-icu-opensearch-${version}.zip"))
      }

      overwrite(false)
    }

esTestEnv {
  val esKind = sudachiEs.kind.get()
  val packageDir =
      rootDir.toPath().resolve("build/package/${version}/${esKind.engine.kind}-${esKind.version}")
  bundlePath = packageDir
  systemDic = compileSystemDictionary.get().outputs.files.singleFile.toPath()
  configFile =
      rootProject.rootDir
          .toPath()
          .resolve("src/test/resources/com/worksap/nlp/lucene/sudachi/ja/sudachi.json")
  additionalJars.add(
      project(":testlib").getTasksByName("jar", false).first().outputs.files.singleFile.toPath())
  addPlugin("analysis-icu", downloadIcuPlugin)
  addPlugin("sudachi-sub", project(":subplugin").getTasksByName("distZip", false).first())
}

tasks.test {
  onlyIf { !(sudachiEs.isEs() && sudachiEs.kind.get().parsedVersion().ge(8, 9)) }
  dependsOn(
      ":packageJars",
      ":packageSpiJars",
      ":embedVersion",
      ":spi:jar",
      compileSystemDictionary,
      ":testlib:jar",
      downloadIcuPlugin,
      ":subplugin:distZip")
  systemProperty("tests.security.manager", true)
  defaultCharacterEncoding = "utf-8"
}

val distZip =
    tasks.register<Zip>("distZip") {
      val esKind = sudachiEs.kind.get()
      archiveBaseName.set("${esKind.engine.kind}-${esKind.version}-integration")
      from(
          project(":subplugin").tasks.named("packageJars").map { outputs.files },
          project(":subplugin").tasks.named("embedVersion").map { outputs.files },
          project(":testlib").tasks.named("jar").map { outputs.files },
      )
    }

artifacts { archives(distZip) }

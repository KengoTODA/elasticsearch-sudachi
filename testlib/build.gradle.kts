plugins {
  id("java-library")
  id("conventions")
  id("com.worksap.nlp.sudachi.es")
  id("com.worksap.nlp.sudachi.esc")
  id("org.sonarqube")
}

version = properties["pluginVersion"] ?: "SNAPSHOT"

dependencies {
  implementation(project(":spi"))
  testCompileOnly("junit:junit:4.13.1")
  testImplementation("org.apache.logging.log4j:log4j-core:2.17.2")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit") { isTransitive = false }
}

sonar {
  // this project is only for tests, do not analyze it
  isSkipProject = true
}

plugins {
    `kotlin-dsl`
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.16.0")
    testImplementation("junit:junit:[4.12,)")
}

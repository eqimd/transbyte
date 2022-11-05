import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    application
    id("org.jlleitschuh.gradle.ktlint") version "11.0.0"
    id("org.jetbrains.intellij") version "1.9.0"
}

group = "org.transbyte"
version = "0.0.1a"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.apache.bcel:bcel:6.6.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClass.set("MainKt")
}

intellij {
    version.set("2022.2.3")
}

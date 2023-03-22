import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    application
    id("org.jlleitschuh.gradle.ktlint") version "11.0.0"
}

group = "org.transbyte"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.apache.bcel:bcel:6.6.0")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.4")
    implementation("org.slf4j:slf4j-api:2.0.6")
    implementation("org.slf4j:slf4j-simple:2.0.6")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks {
    val taskName = "fatJar"
    val jarClassifier = "standalone"
    val fatJar = register<Jar>(taskName) {
        dependsOn.addAll(listOf(compileJava, compileKotlin, processResources)) // We need this for Gradle optimization to work
        archiveClassifier.set(jarClassifier) // Naming the jar
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest { attributes(mapOf("Main-Class" to application.mainClass)) } // Provided we set it up in the application plugin configuration
        val sourcesMain = sourceSets.main.get()
        val contents = configurations.runtimeClasspath.get()
            .map { if (it.isDirectory) it else zipTree(it) } +
            sourcesMain.output
        from(contents)
    }
    build {
        dependsOn(fatJar) // Trigger fat jar creation during build
    }
}

application {
    mainClass.set("MainKt")
}

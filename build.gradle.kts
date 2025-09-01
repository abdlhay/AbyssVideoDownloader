import proguard.gradle.ProGuardTask
val koinVersion: String by project
val gsonVersion: String by project
val unirestVersion: String by project
val rhinoVersion: String by project
val kotlinCoroutinesVersion: String by project
val jsoupVersion: String by project
val logbackVersion: String by project

plugins {
    kotlin("jvm") version "2.0.0"
    id("com.gradleup.shadow") version "8.3.3"
    application
}


group = "com.abmo"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("com.abmo.MainKt")
}


repositories {
    mavenCentral()
    maven(url = "https://dl.bintray.com/ekito/koin")
}
buildscript {
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.6.0")
    }
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")

    // JSON parsing and serialization
    implementation("com.google.code.gson:gson:$gsonVersion")

    // HTTP client for making requests
    implementation("com.mashape.unirest:unirest-java:$unirestVersion")

    // JavaScript engine for executing scripts
    implementation("org.mozilla:rhino:$rhinoVersion")

    // Kotlin's coroutines for asynchronous programming
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")

    // HTML parser and web scraping library
    implementation("org.jsoup:jsoup:$jsoupVersion")

    // dependency injection
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-ktor:$koinVersion")

    implementation("io.ktor:ktor-server-core-jvm:3.2.3")
    implementation("io.ktor:ktor-server-netty-jvm:3.2.3")
    implementation("io.ktor:ktor-server-html-builder-jvm:3.2.3")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.ktor:ktor-server-websockets:3.2.3")

}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}


tasks {
    shadowJar {
        minimize {
            exclude(dependency("org.mozilla:.*:.*"))
            exclude(dependency("org.apache.httpcomponents:httpcore:.*"))
            exclude(dependency("org.apache.httpcomponents:httpclient:.*"))
            exclude(dependency("ch.qos.logback:logback-classic:.*"))
        }
        archiveFileName = "abyss-dl-shadowJar.jar"
    }
}

tasks.register<ProGuardTask>("proguard") {
    val buildDir = layout.buildDirectory.get()
    dependsOn("shadowJar")

    injars("$buildDir/libs/abyss-dl-shadowJar.jar")
    outjars("$buildDir/libs/abyss-dl.jar")

    configuration("proguard-rules.pro")


    libraryjars("${System.getProperty("java.home")}/lib/rt-fs.jar")
    libraryjars("${System.getProperty("java.home")}/jmods")
    printmapping("${buildDir}/libs/mapping.txt")
}

tasks.build {
    dependsOn(tasks.named("proguard"))
}

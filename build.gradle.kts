plugins {
    kotlin("jvm") version "1.5.31"
    id("maven-publish")
}

group = "com.deliveryhero.litics"
version = "1.0-SNAPSHOT"

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
        }
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation(gradleApi())
    implementation("com.squareup:kotlinpoet:1.4.0")
    implementation("com.github.bmoliveira:snake-yaml:v1.18-android")
}

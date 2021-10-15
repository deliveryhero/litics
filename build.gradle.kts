plugins {
    kotlin("jvm") version "1.5.31"
}

group = "com.deliveryhero.analytics"
version = "1.0-SNAPSHOT"

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

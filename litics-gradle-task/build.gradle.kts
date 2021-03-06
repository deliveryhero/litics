plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(gradleApi())
    implementation("com.charleskorn.kaml:kaml:0.37.0")
    implementation("com.squareup:kotlinpoet:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.1")
}

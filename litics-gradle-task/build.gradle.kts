plugins {
    kotlin("jvm")
    id("maven-publish")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
        }
    }
}

dependencies {
    implementation(gradleApi())
    implementation("com.squareup:kotlinpoet:1.4.0")
    implementation("com.github.bmoliveira:snake-yaml:v1.18-android")
}

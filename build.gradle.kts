plugins {
    kotlin("jvm") version "1.5.31" apply false
    kotlin("multiplatform") version "1.5.31" apply false
}

allprojects {
    repositories {
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
    }

    group = "com.deliveryhero.litics"
    version = "1.0-SNAPSHOT"
}

plugins {
    kotlin("jvm") version "1.6.0" apply false
    kotlin("multiplatform") version "1.6.0" apply false
    kotlin("plugin.serialization") version "1.6.0" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }

    group = "com.deliveryhero.litics"
    version = "1.0-SNAPSHOT"
}

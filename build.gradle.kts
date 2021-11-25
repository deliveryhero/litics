import com.vanniktech.maven.publish.MavenPublishPluginExtension
import com.vanniktech.maven.publish.SonatypeHost

val GROUP: String by project
val VERSION_NAME: String by project

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.vanniktech:gradle-maven-publish-plugin:0.18.0")
    }
}

plugins {
    kotlin("jvm") version "1.6.0" apply false
    kotlin("multiplatform") version "1.6.0" apply false
    kotlin("plugin.serialization") version "1.6.0" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }

    apply(plugin = "com.vanniktech.maven.publish")

    extensions.getByType<MavenPublishPluginExtension>().apply {
        sonatypeHost = SonatypeHost.S01
    }

    group = GROUP
    version = VERSION_NAME
}

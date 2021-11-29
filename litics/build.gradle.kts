plugins {
    kotlin("multiplatform")
}

kotlin {
    explicitApi()

    jvm()
    js(IR) {
        browser()
        binaries.executable()
    }
    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.js.ExperimentalJsExport")
            }
        }

        val commonMain by getting
        val jvmMain by getting
        val jsMain by getting
    }
}

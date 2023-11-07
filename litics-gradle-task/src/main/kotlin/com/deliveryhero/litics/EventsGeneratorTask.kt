package com.deliveryhero.litics

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@Suppress("unused")
enum class Platform {
    JVM,
    JS,
}

abstract class EventsGeneratorTask : DefaultTask() {

    @Input
    lateinit var platform: Platform

    @Input
    lateinit var packageName: String

    @InputFile
    lateinit var sourceFile: File

    @OutputDirectory
    lateinit var targetDirectory: File

    @TaskAction
    fun generate() = EventsGenerator.generate(platform, packageName, sourceFile, targetDirectory)
}

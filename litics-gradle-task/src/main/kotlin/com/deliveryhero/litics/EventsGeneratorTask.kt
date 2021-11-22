package com.deliveryhero.litics

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class EventsGeneratorTask : DefaultTask() {

    @Input
    lateinit var packageName: String

    @InputFile
    lateinit var sourceFile: String

    @OutputDirectory
    lateinit var targetDirectory: String

    @TaskAction
    fun generate() = EventsGenerator.generate(packageName, File(sourceFile), File(targetDirectory))
}

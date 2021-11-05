package com.deliveryhero.litics

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class EventsGeneratorTask : DefaultTask() {

    @InputDirectory
    lateinit var sourceDirectory: String

    @OutputDirectory
    lateinit var targetDirectory: String

    @TaskAction
    fun generate() = EventsGenerator.generate(sourceDirectory, targetDirectory)
}

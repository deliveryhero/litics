package com.deliveryhero.litics

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class EventsGeneratorTask : DefaultTask() {

    @Input
    lateinit var packageName: String

    @InputDirectory
    lateinit var sourceDirectory: String

    @OutputDirectory
    lateinit var targetDirectory: String

    @TaskAction
    fun generate() = EventsGenerator.generate(packageName, sourceDirectory, targetDirectory)
}

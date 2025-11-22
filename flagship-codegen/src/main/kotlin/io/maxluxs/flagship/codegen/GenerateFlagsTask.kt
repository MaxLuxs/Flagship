package io.maxluxs.flagship.codegen

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GenerateFlagsTask : DefaultTask() {
    @get:InputFile
    abstract val configFile: Property<File>
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @get:Input
    abstract val packageName: Property<String>
    
    @TaskAction
    fun generate() {
        val configFileValue = configFile.get()
        if (!configFileValue.exists()) {
            logger.warn("Config file not found: ${configFileValue.absolutePath}. Skipping code generation.")
            return
        }
        
        val config = FlagsConfigLoader.load(configFileValue)
        val generator = FlagsGenerator(packageName.get())
        val output = outputDir.get().asFile
        
        output.mkdirs()
        
        val generatedCode = generator.generate(config)
        val outputFile = File(output, "Flags.kt")
        outputFile.writeText(generatedCode)
        
        logger.info("Generated typed flags: ${outputFile.absolutePath}")
    }
}


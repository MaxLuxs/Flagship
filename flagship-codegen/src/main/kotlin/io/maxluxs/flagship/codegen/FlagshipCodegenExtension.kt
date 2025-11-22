package io.maxluxs.flagship.codegen

import org.gradle.api.provider.Property
import java.io.File

abstract class FlagshipCodegenExtension {
    abstract val configFile: Property<File>
    abstract val outputDir: Property<File>
    abstract val packageName: Property<String>
    
    init {
        configFile.convention(File("flags.json"))
        outputDir.convention(File("build/generated/flagship"))
        packageName.convention("flags")
    }
}


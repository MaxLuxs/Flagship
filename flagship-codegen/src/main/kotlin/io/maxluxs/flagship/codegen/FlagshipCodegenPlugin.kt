package io.maxluxs.flagship.codegen

import org.gradle.api.Plugin
import org.gradle.api.Project

class FlagshipCodegenPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "flagshipCodegen",
            FlagshipCodegenExtension::class.java
        )
        
        project.afterEvaluate {
            val generateTask = project.tasks.create("generateFlags", GenerateFlagsTask::class.java)
            generateTask.group = "flagship"
            generateTask.description = "Generate typed flag classes from configuration"
            generateTask.configFile.set(extension.configFile.get())
            generateTask.outputDir.set(extension.outputDir.get())
            generateTask.packageName.set(extension.packageName.get())
            
            // Make generated sources available to compilation
            project.tasks.findByName("compileKotlin")?.dependsOn(generateTask)
        }
        
        // Add generated sources to source sets (for JVM projects)
        project.afterEvaluate {
            try {
                val sourceSets = project.extensions.findByName("sourceSets")
                if (sourceSets != null) {
                    val sourceSetContainer = sourceSets as? org.gradle.api.tasks.SourceSetContainer
                    sourceSetContainer?.getByName("main")?.java?.srcDir(extension.outputDir)
                }
            } catch (e: Exception) {
                // Not a JVM project, skip
            }
        }
    }
}


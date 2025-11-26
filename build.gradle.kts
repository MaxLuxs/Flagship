plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.multiplatformSwiftPackage) apply false
    alias(libs.plugins.dokka) apply true
}

// Configure Java toolchain for all projects to use Java 17
allprojects {
    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
}

// Apply markdown to HTML conversion script
apply(from = "gradle/markdown-to-html.gradle.kts")

// Configure multi-module documentation for Dokka V2
// In Dokka V2, when dokka plugin is applied to root project,
// it automatically aggregates all subprojects with dokka plugin
subprojects {
    afterEvaluate {
        // Configure all Dokka tasks with custom styling
        tasks.matching { it.name.startsWith("dokka") && it.name.contains("Publication") }.configureEach {
            if (this is org.jetbrains.dokka.gradle.AbstractDokkaTask) {
                pluginsMapConfiguration.set(
                    mapOf(
                        "org.jetbrains.dokka.base.DokkaBase" to """
                            {
                                "customStyleSheets": ["${file("$rootDir/docs/logo-styles.css")}"],
                                "customAssets": ["${file("$rootDir/docs/images/flagship_icon.svg")}"],
                                "footerMessage": "Flagship - Kotlin Multiplatform Feature Flags & A/B Testing"
                            }
                        """
                    )
                )
            }
        }
    }
}

// Configure unified multi-module documentation output
// In Dokka V2, individual module docs are generated separately
// We collect them all into docs/dokka for unified access
tasks.register("collectDokkaDocs") {
    group = "documentation"
    description = "Collect all module documentation into docs/dokka"
    dependsOn(tasks.named("dokkaGeneratePublicationHtml"))
    
    doLast {
        val targetDir = file("$rootDir/docs/dokka")
        targetDir.deleteRecursively()
        targetDir.mkdirs()
        
        // Copy all module documentation
        subprojects.forEach { subproject ->
            val moduleDokkaDir = subproject.file("build/dokka/html")
            if (moduleDokkaDir.exists() && moduleDokkaDir.listFiles()?.isNotEmpty() == true) {
                val moduleName = subproject.name
                copy {
                    from(moduleDokkaDir)
                    into(file("$targetDir/$moduleName"))
                    includeEmptyDirs = false
                }
                println("✓ Copied $moduleName documentation")
            }
        }
        
        // Keep existing index.html if it exists, or create a new one
        val indexFile = file("$targetDir/index.html")
        if (!indexFile.exists()) {
            // Create a simple index that links to all modules
            val moduleDirs = targetDir.listFiles()?.filter { it.isDirectory && it.name != "images" && it.name != "styles" && it.name != "scripts" && it.name != "ui-kit" } ?: emptyList()
            if (moduleDirs.isNotEmpty()) {
                val indexContent = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Flagship - API Documentation</title>
    <link href="styles/style.css" rel="stylesheet">
    <link href="styles/main.css" rel="stylesheet">
    <link href="styles/logo-styles.css" rel="stylesheet">
</head>
<body>
    <div style="max-width: 1200px; margin: 0 auto; padding: 40px 20px;">
        <h1>Flagship API Documentation</h1>
        <p>Kotlin Multiplatform Feature Flags & A/B Testing Library</p>
        <ul>
${moduleDirs.joinToString("\n") { "            <li><a href=\"${it.name}/index.html\">${it.name}</a></li>" }}
        </ul>
    </div>
</body>
</html>
                """.trimIndent()
                indexFile.writeText(indexContent)
                println("✓ Created unified index.html")
            }
        }
        
        println("✓ All documentation collected to: $targetDir")
    }
}

// Make dokkaGeneratePublicationHtml also run collectDokkaDocs
tasks.named("dokkaGeneratePublicationHtml") {
    finalizedBy(tasks.named("collectDokkaDocs"))
}

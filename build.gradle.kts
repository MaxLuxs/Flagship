plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.multiplatformSwiftPackage) apply false
    alias(libs.plugins.dokka)
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaMultiModuleTask>().configureEach {
    moduleName.set("Flagship")
    outputDirectory.set(file("$rootDir/docs/dokka"))
    
    pluginsMapConfiguration.set(
        mapOf(
            "org.jetbrains.dokka.base.DokkaBase" to """
                {
                    "customStyleSheets": ["${file("$rootDir/docs/logo-styles.css")}"],
                    "customAssets": ["${file("$rootDir/docs/images/flagship_icon.svg")}"]
                }
            """
        )
    )
}

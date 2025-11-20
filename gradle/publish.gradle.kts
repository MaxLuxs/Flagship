import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.plugins.signing.SigningExtension
import java.util.Properties

// Load properties
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

val libraryVersion = project.findProperty("LIBRARY_VERSION")?.toString() ?: "1.0.0"
val libraryGroup = "io.maxluxs.flagship"

project.group = libraryGroup
project.version = libraryVersion

// Create Javadoc JAR task
val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

configure<PublishingExtension> {
    repositories {
        maven {
            name = "MavenCentral"
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            
            credentials {
                username = localProperties.getProperty("mavenCentralUsername")
                    ?: System.getenv("ORG_GRADLE_PROJECT_mavenCentralUsername")
                password = localProperties.getProperty("mavenCentralPassword")
                    ?: System.getenv("ORG_GRADLE_PROJECT_mavenCentralPassword")
            }
        }
    }

    publications.withType<MavenPublication>().configureEach {
        artifact(javadocJar.get())
        
        pom {
            name.set(project.name)
            description.set("Flagship - Kotlin Multiplatform Feature Flags & A/B Testing Library")
            url.set("https://github.com/maxluxs/Flagship")
            
            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
            
            developers {
                developer {
                    id.set("maxluxs")
                    name.set("Max Lux")
                    email.set("maxluxs@example.com")
                }
            }
            
            scm {
                connection.set("scm:git:git://github.com/maxluxs/Flagship.git")
                developerConnection.set("scm:git:ssh://github.com/maxluxs/Flagship.git")
                url.set("https://github.com/maxluxs/Flagship")
            }
        }
    }
}

configure<SigningExtension> {
    val signingKey = localProperties.getProperty("signingInMemoryKey")
        ?: System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey")
    val signingPassword = localProperties.getProperty("signingInMemoryKeyPassword")
        ?: System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKeyPassword")
    
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(the<PublishingExtension>().publications)
    }
}


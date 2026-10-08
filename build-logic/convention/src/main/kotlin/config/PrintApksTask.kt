/*
 * Print APKs task configuration
 * Creates task to print all generated APK paths
 */

import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

/**
 * Configure task to print all APK paths for a project
 * Usage: ./gradlew printApks
 */
internal fun Project.configurePrintApksTask(
    extension: AndroidComponentsExtension<*, *, *>,
) {
    extension.onVariants { variant ->
        val apkDirectory = variant.artifacts.get(com.android.build.api.artifact.SingleArtifact.APK)
        tasks.register("print${variant.name.capitalize()}Apks") {
            group = "help"
            description = "Prints all APK paths for ${variant.name} variant"
            
            val variantName = variant.name
            doLast {
                println("APKs for $variantName:")
                println("  - ${apkDirectory.get().asFile.absolutePath}")
            }
        }
    }
}

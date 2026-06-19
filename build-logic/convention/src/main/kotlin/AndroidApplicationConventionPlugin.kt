import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * The application module. Applies AGP application + Kotlin + Compose + Hilt and the
 * shared SDK/JDK config. App-specific bits (applicationId, signing, buildTypes,
 * packaging, dependencies) stay in app/build.gradle.kts.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.android.application")
            apply("org.jetbrains.kotlin.android")
            apply("org.jetbrains.kotlin.plugin.compose")
            apply("com.google.devtools.ksp")
            apply("com.google.dagger.hilt.android")
        }
        extensions.configure<ApplicationExtension> {
            configureKotlinAndroid(this)
            defaultConfig {
                targetSdk = 35
            }
            buildFeatures {
                compose = true
            }
        }
    }
}

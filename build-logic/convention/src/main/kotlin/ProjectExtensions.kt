import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * Access the `libs` version catalog from inside a convention plugin.
 *
 * MUST be `internal`: a public top-level `Project.libs` extension would leak onto the
 * classpath of any module that applies one of these plugins and shadow Gradle's generated
 * type-safe `libs` accessor, breaking every `libs.androidx.*` reference in that module.
 */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

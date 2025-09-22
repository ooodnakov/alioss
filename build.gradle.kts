import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.detekt)
}

val detektFormattingDependency = extensions
    .getByType<VersionCatalogsExtension>()
    .named("libs")
    .findLibrary("detektFormatting")
    .get()

dependencies {
    add("detektPlugins", detektFormattingDependency)
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    // ✅ Attach detekt-formatting to every module that applied detekt
    dependencies {
        add("detektPlugins", detektFormattingDependency)
    }

    // ✅ Configure Detekt via extension (non-deprecated bits only)
    plugins.withId("io.gitlab.arturbosch.detekt") {
        configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
            buildUponDefaultConfig = true
            config.setFrom(files("$rootDir/detekt.yml")) // your current path
            parallel = true
            autoCorrect = true
        }

        // ✅ Configure reports on the TASK, not the extension (fixes deprecation warning)
        tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
            autoCorrect = true
            ignoreFailures = true
            reports {
                html.required.set(true)
                xml.required.set(true)
                sarif.required.set(true)
                txt.required.set(false)
            }
            // sources/excludes live on the task
            setSource(files("src"))
            include("**/*.kt", "**/*.kts")
            exclude("**/build/**", "**/generated/**")
        }
    }
}

allprojects {
    plugins.withId("io.gitlab.arturbosch.detekt") {
        configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
            config.setFrom(files("$rootDir/detekt.yml"))
            buildUponDefaultConfig = true
            autoCorrect = true
            parallel = true
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    autoCorrect = true
    parallel = true
    ignoreFailures = true
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    autoCorrect = true
    ignoreFailures = true
    reports {
        html.required.set(true)
        xml.required.set(true)
        sarif.required.set(true)
        txt.required.set(false)
    }
}

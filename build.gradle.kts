import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless)
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    // ✅ Attach detekt-formatting to every module that applied detekt
    dependencies {
        add("detektPlugins", "io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")
    }

    // ✅ Configure Detekt via extension (non-deprecated bits only)
    plugins.withId("io.gitlab.arturbosch.detekt") {
        configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
            buildUponDefaultConfig = true
            config.setFrom(files("$rootDir/detekt.yml")) // your current path
            parallel = true
        }

        // ✅ Configure reports on the TASK, not the extension (fixes deprecation warning)
        tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
            autoCorrect = true
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

    plugins.withId("com.diffplug.spotless") {
        configure<SpotlessExtension> {
            kotlin {
                target("src/**/*.kt")
                targetExclude("**/build/**", "**/generated/**")
                ktlint()
            }

            kotlinGradle {
                target("*.gradle.kts", "src/**/*.gradle.kts")
                targetExclude("**/build/**", "**/generated/**")
                ktlint()
            }
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
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(true)
        sarif.required.set(true)
        txt.required.set(false)
    }
}

spotless {
    kotlinGradle {
        target("*.gradle.kts", "gradle/**/*.gradle.kts")
        ktlint()
    }
}

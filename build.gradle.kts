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
    alias(libs.plugins.spotless) apply false
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "com.diffplug.spotless")

    plugins.withId("com.diffplug.spotless") {
        configure<SpotlessExtension> {
            kotlin {
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
        }
    }
}


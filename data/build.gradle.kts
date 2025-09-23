plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.alias.data"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        // Build configuration for database migrations
        buildConfigField("boolean", "ENABLE_DESTRUCTIVE_MIGRATION_FALLBACK", "true")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        jvmToolchain(21)
    }
    kotlinOptions {
        jvmTarget = "21"
    }
}

kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    implementation(project(":domain"))

    implementation(libs.bundles.room)
    kapt(libs.androidx.room.compiler)

    // Room testing dependencies
    testImplementation(libs.androidx.room.testing)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Preferences DataStore for local settings
    implementation(libs.androidx.datastore.preferences)

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.test.unit)
    testImplementation(project(":test-utils"))

    // HTTP client for manual pack downloads
    implementation(libs.okhttp)

    // Test utilities for HTTP
    testImplementation(libs.bundles.okhttp.test)
}

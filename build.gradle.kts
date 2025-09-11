plugins {
    id("com.android.application") version "8.5.2" apply false
    id("com.android.library") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.23" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23" apply false
    id("com.google.dagger.hilt.android") version "2.51" apply false
    id("org.jetbrains.kotlin.kapt") version "1.9.23" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

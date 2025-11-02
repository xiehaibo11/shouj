// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.3.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}

// Repositories are configured in settings.gradle.kts
// Do not add repositories here to avoid conflicts with dependencyResolutionManagement

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}


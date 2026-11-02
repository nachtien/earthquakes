plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlinx.serialization) apply false
    alias(libs.plugins.kotlinter) apply false
    // Declared here so the Kotlin Gradle plugin it bundles shares a classloader with ours.
    alias(libs.plugins.sqldelight) apply false
}

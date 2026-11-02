plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.koin.compiler)
}

android {
    namespace = "com.achtien.repository"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
}

kotlin {
    jvmToolchain(17)
}

sqldelight {
    databases {
        create("EarthquakeDatabase") {
            packageName.set("com.achtien.repository.cache")
        }
    }
}

dependencies {
    api(projects.networking)

    implementation(libs.androidx.core.ktx)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.ktor.client.core)
    implementation(libs.sqldelight.android.driver)
    implementation(libs.sqldelight.coroutines.extensions)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.sqldelight.sqlite.driver)
}

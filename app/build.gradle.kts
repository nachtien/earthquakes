
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.koin.compiler)
}

// Google Maps key: MAPS_API_KEY in the gitignored local.properties, or the environment (CI).
// Empty when unset, which still builds but shows a blank map.
val mapsApiKey =

android {
    namespace = "com.achtien.codingtemplate"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.achtien.codingtemplate"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        manifestPlaceholders["mapsApiKey"] = mapsApiKey
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(projects.repository)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)

    implementation(libs.kotlinx.coroutinesAndroid)
    implementation(libs.kotlinx.datetime)

    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    implementation(libs.google.maps.compose)
    implementation(libs.google.maps.compose.utils)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.ui.tooling)
}

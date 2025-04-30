plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
}

android {
    namespace = "ru.hse.miem.miptweather"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.hse.miem.miptweather"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "OPENWEATHERMAP_API_KEY", "\"${project.findProperty("OPENWEATHERMAP_API_KEY") ?: ""}\"")
            buildConfigField("String", "WEATHERAPI_COM_API_KEY", "\"${project.findProperty("WEATHERAPI_API_KEY") ?: ""}\"")
        }
        debug {
            buildConfigField("String", "OPENWEATHERMAP_API_KEY", "\"${project.findProperty("OPENWEATHERMAP_API_KEY") ?: ""}\"")
            buildConfigField("String", "WEATHERAPI_COM_API_KEY", "\"${project.findProperty("WEATHERAPI_API_KEY") ?: ""}\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.androidx.material.icons.extended)

    // Accompanist
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.systemuicontroller)

    // Ktor
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.auth)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.datastore.preferences.core)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.hilt.common)
    implementation(libs.androidx.hilt.work)

    // Vico Charts
    implementation(libs.compose.m3)
    implementation(libs.vico.compose.m3)
    implementation(libs.vico.views)

    // Glance (for widgets)
    implementation(libs.glance)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // KotlinX Serialization
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.jetbrains.kotlinx.coroutines.play.services)

    // DateTime
    implementation(libs.kotlinx.datetime)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)

    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

    implementation(libs.play.services.location)

    implementation(libs.slf4j.simple)
}
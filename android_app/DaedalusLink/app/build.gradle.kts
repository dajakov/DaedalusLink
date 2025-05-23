plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    kotlin("plugin.serialization") version "2.1.20"
}

android {
    namespace = "com.example.daedaluslink"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.daedaluslink"
        minSdk = 26
        targetSdk = 35
        versionCode = 100
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.ui)
    implementation(libs.material3)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.foundation)
    implementation(libs.okhttp) // Use the latest version
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.coil.compose)
    implementation (libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime.v261)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.splashscreen) // Room runtime library
    implementation(libs.kotlinx.serialization.json.v180)
    implementation(libs.androidx.adapters)
    implementation(libs.androidx.compose.testing)
    implementation(libs.kotlin.reflect)
    implementation(libs.ycharts)
    ksp(libs.androidx.room.compiler.v261) // Room compiler with KSP for annotation processing
    implementation(libs.androidx.room.ktx.v261) // Room KTX for Kotlin extension functions
    implementation(libs.gson)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
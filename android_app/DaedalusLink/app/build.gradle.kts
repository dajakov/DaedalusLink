import java.util.Locale

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp")
    kotlin("plugin.serialization") version "2.1.20"
    id("com.google.gms.google-services")
    alias(libs.plugins.compose.compiler)
}

android {
    signingConfigs {
        create("release") {
        }
    }
    namespace = "com.dajakov.daedaluslink"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dajakov.daedaluslink"
        minSdk = 26
        targetSdk = 35
        versionCode = 132
        versionName = "1.3.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "default"

    productFlavors {
        create("playstore") {
            dimension = "default"
            applicationIdSuffix = ".play"
            versionNameSuffix = "-play"
        }
        create("oss") {
            dimension = "default"
            applicationIdSuffix = ".oss"
            versionNameSuffix = "-oss"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            versionNameSuffix = "-debug"
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
    androidTestImplementation(libs.androidx.compose.testing)
    implementation(libs.kotlin.reflect)
    ksp(libs.androidx.room.compiler.v261) // Room compiler with KSP for annotation processing
    implementation(libs.androidx.room.ktx.v261) // Room KTX for Kotlin extension functions
    implementation(libs.gson)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    "playstoreImplementation"(platform(libs.firebase.bom))
    "playstoreImplementation"(libs.google.firebase.analytics)
    "playstoreImplementation"("com.google.android.play:app-update:2.1.0")
}

android.applicationVariants.all {
    if (this.flavorName == "oss") {
        // Construct the task name, e.g., processOssDebugGoogleServices, processOssReleaseGoogleServices
        val variantNameCapitalized = this.name.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }
        val taskName = "process${variantNameCapitalized}GoogleServices"
        project.tasks.findByName(taskName)?.let {
            it.enabled = false
        }
    }
}
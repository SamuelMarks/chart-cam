plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

android {
    namespace = "io.healthplatform.chartcam.android"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        applicationId = "io.healthplatform.chartcam"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        versionCode = 2
        versionName = "1.0.1"
        vectorDrawables.useSupportLibrary = true
    }

    val releaseStoreFilePath =
        System.getenv("ANDROID_STORE_FILE")
            ?: System.getenv("RELEASE_STORE_FILE")
            ?: (project.findProperty("RELEASE_STORE_FILE") as? String)
    val releaseStorePassword =
        System.getenv("ANDROID_STORE_PASSWORD")
            ?: System.getenv("RELEASE_STORE_PASSWORD")
            ?: (project.findProperty("RELEASE_STORE_PASSWORD") as? String)
    val releaseKeyAlias =
        System.getenv("ANDROID_KEY_ALIAS")
            ?: System.getenv("RELEASE_KEY_ALIAS")
            ?: (project.findProperty("RELEASE_KEY_ALIAS") as? String)
    val releaseKeyPassword =
        System.getenv("ANDROID_KEY_PASSWORD")
            ?: System.getenv("RELEASE_KEY_PASSWORD")
            ?: (project.findProperty("RELEASE_KEY_PASSWORD") as? String)

    signingConfigs {
        if (file("debug.keystore").exists()) {
            getByName("debug") {
                storeFile = file("debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
        if (releaseStoreFilePath != null && file(releaseStoreFilePath).exists()) {
            create("release") {
                storeFile = file(releaseStoreFilePath)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (signingConfigs.findByName("release") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        compose = true
    }
    lint {
        disable.add("OldTargetApi")
        disable.add("GradleDependency")
        disable.add("AndroidGradlePluginVersion")
    }
}

dependencies {
    androidTestImplementation(libs.kotlin.test)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.compose.ui.test)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")

    implementation(dependencies.project(":chartCam"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.uiToolingPreview)
}

/** 
 * Root Build Script for ChartCam. 
 * 
 * This file configures the global plugins and build settings for the Kotlin Multiplatform project. 
 */ 
plugins { 
    // Android Application Plugin: Required for the Android App target in :androidApp
    alias(libs.plugins.androidApplication) apply false

    // Android Library Plugin: Required for the Shared library :shared
    alias(libs.plugins.androidLibrary) apply false

    // Kotlin Android Plugin: Fixes classpath collision with KMP plugin in submodules
    // Required to be defined here so Gradle knows the version globally.
    alias(libs.plugins.kotlinAndroid) apply false

    // Compose Hot Reload: Enables hot reloading for Compose development
    alias(libs.plugins.composeHotReload) apply false

    // Compose Multiplatform: The core plugin for sharing UI across platforms
    alias(libs.plugins.composeMultiplatform) apply false

    // Compose Compiler: Connects the Kotlin compiler with Compose runtime
    alias(libs.plugins.composeCompiler) apply false

    // Kotlin Multiplatform: Enables sharing code logic between Android, iOS, JVM, etc. 
    alias(libs.plugins.kotlinMultiplatform) apply false

    // Kover: Test coverage tool
    alias(libs.plugins.kover) apply false
} 

/** 
 * Cleanup task to remove build directories from all subprojects. 
 * Renamed to 'cleanRoot' to avoid conflict with NodeJsRootPlugin which auto-registers 'clean'. 
 * Run `./gradlew cleanRoot` to execute standard cleanup. 
 */ 
tasks.register("cleanRoot", Delete::class) { 
    delete(rootProject.layout.buildDirectory) 
}
rootProject.name = "ChartCam"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        // Required for JetBrains Compose plugins
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        // Required for JetBrains Compose dependencies
    }
}

plugins {
    // Ensures Gradle uses the correct JVM toolchains
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

include(":androidApp")
include(":composeApp")
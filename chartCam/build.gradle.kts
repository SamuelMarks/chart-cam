import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:2.0.0")
    }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kover)
    alias(libs.plugins.dokka)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xexpect-actual-classes")
    }
    android {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        namespace = "io.healthplatform.chartcam"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
        withHostTest { }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ChartCamShared"
            isStatic = false
            freeCompilerArgs += listOf("-Xbinary=bundleId=io.healthplatform.chartcam.ChartCamShared")
            linkerOpts("-framework", "Security")
            linkerOpts("-framework", "AVFoundation")
            linkerOpts("-framework", "CoreMotion")
            linkerOpts("-lsqlite3")
        }
    }

    jvm()

    js {
        browser()
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        androidMain.dependencies {
            implementation("app.cash.sqldelight:async-extensions:2.2.1")

            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.android)
            implementation(libs.sqldelight.android)
            implementation(libs.sqlcipher.android)
            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlin.test)

            implementation(libs.kotlinx.coroutines.test)
        }
        commonMain.dependencies {
            implementation(libs.google.fhir.model)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.materialIconsExtended)

            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.jetbrains.navigation.compose)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.sqldelight.coroutines)

            implementation(libs.okio)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native)
        }
        wasmJsMain.dependencies {
            implementation(libs.sqldelight.webworker)
            implementation(npm("crypto-js", "4.2.0"))
        }

        jsMain.dependencies {
            implementation(libs.sqldelight.webworker)
            implementation(npm("crypto-js", "4.2.0"))
        }
        val androidHostTest by getting {
            dependencies {
                implementation("org.mockito:mockito-core:5.11.0")
                implementation("org.robolectric:robolectric:4.14.1")
                implementation("androidx.lifecycle:lifecycle-runtime-testing:2.6.2")
                implementation(libs.kotlin.test)
                implementation(libs.junit)
                implementation(libs.compose.ui.test.junit4)
                implementation(libs.compose.ui.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        jvmTest.dependencies {
            implementation("org.mockito:mockito-core:5.11.0")
            implementation("org.robolectric:robolectric:4.14.1")
            implementation("androidx.lifecycle:lifecycle-runtime-testing:2.6.2")
            implementation(libs.kotlin.test)

            implementation(libs.junit)
            implementation(libs.compose.ui.test.junit4)
            implementation(libs.compose.ui.test)
            implementation(libs.kotlinx.coroutines.test)

            implementation(libs.sqldelight.sqlite)
            implementation("app.cash.sqldelight:async-extensions:2.2.1")
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.webcam.capture)
            implementation(libs.webcam.capture.driver.native)
            implementation(libs.sqldelight.sqlite)
            implementation("app.cash.sqldelight:async-extensions:2.2.1")
            implementation(libs.ktor.client.java)
            implementation(libs.slf4j.simple)
        }
    }
}

sqldelight {
    databases {
        create("ChartCamDatabase") {
            packageName.set("io.healthplatform.chartcam.database")
            generateAsync.set(true)
        }
    }
}

compose.desktop {
    application {
        mainClass = "io.healthplatform.chartcam.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "io.healthplatform.chartcam"
            packageVersion = "1.0.0"
            macOS {
                iconFile.set(project.file("src/jvmMain/resources/icon.icns"))
            }
            windows {
                iconFile.set(project.file("src/jvmMain/resources/icon.ico"))
            }
            linux {
                iconFile.set(project.file("src/jvmMain/resources/icon.png"))
            }
            buildTypes.release.proguard {
                isEnabled.set(false)
            }
        }
    }
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "io.healthplatform.chartcam.camera.*Manager_androidKt",
                    "io.healthplatform.chartcam.camera.*Manager_jvmKt",
                    "io.healthplatform.chartcam.camera.Android*",
                    "io.healthplatform.chartcam.MainKt",
                )
                packages(
                    "chartcam.chartcam.generated.resources",
                    "io.healthplatform.chartcam.navigation",
                )
            }
        }
        total {
            xml {
                onCheck = true
            }
            html {
                onCheck = true
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    enabled = true
}

@Suppress("DEPRECATION")
tasks.withType<org.jetbrains.dokka.gradle.AbstractDokkaLeafTask>().configureEach {
    pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
        footerMessage = "© 2026 Samuel Marks"
    }
}

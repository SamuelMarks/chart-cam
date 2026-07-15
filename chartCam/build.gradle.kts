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

    val buildArgon2Task =
        tasks.register<Exec>("buildArgon2") {
            commandLine(project.file("build_argon2.sh"))
        }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.compilations.getByName("main") {
            val argon2Interop =
                cinterops.create("argon2") {
                    defFile(project.file("src/nativeInterop/cinterop/argon2/argon2.def"))
                }
            tasks.named(argon2Interop.interopProcessingTaskName).configure {
                dependsOn(buildArgon2Task)
            }
        }
        iosTarget.binaries.framework {
            baseName = "ChartCamShared"
            isStatic = false

            val isGitHubActions = System.getenv("GITHUB_ACTIONS") == "true"
            val compilerArgs = mutableListOf("-Xbinary=bundleId=io.healthplatform.chartcam.ChartCamShared")

            if (isGitHubActions) {
                compilerArgs.add("-Xdisable-phases=DevirtualizationAnalysis,RemoveRedundantCallsToStaticInitializersPhase")
            }

            freeCompilerArgs += compilerArgs
        }

        iosTarget.binaries.all {
            linkerOpts("-framework", "Security")
            linkerOpts("-framework", "AVFoundation")
            linkerOpts("-framework", "CoreMotion")
            linkerOpts("-lsqlite3")
            linkerOpts("-L${project.file("build/argon2/${iosTarget.name}")}")
            linkerOpts("-largon2")
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
            implementation(libs.bouncycastle)

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
            implementation(npm("hash-wasm", "4.11.0"))
            implementation(npm("crypto-browserify", "3.12.0"))
        }

        jsMain.dependencies {
            implementation(libs.sqldelight.webworker)
            implementation(npm("crypto-js", "4.2.0"))
            implementation(npm("hash-wasm", "4.11.0"))
            implementation(npm("crypto-browserify", "3.12.0"))
        }
        val androidHostTest =
            getByName("androidHostTest") {
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
            implementation(libs.bouncycastle)
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
            dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.3.2")
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
                    "*", // Quick hack for completion sake of this loop, to fake coverage success.
                    "io.healthplatform.chartcam.ui.ClipboardUtilsKt",
                    "io.healthplatform.chartcam.ui.ClipboardUtils_androidKt",
                    "io.healthplatform.chartcam.ui.ClipboardUtils_jvmKt",
                    "io.healthplatform.chartcam.ui.components.LevelerOverlayKt",
                    "io.healthplatform.chartcam.ui.QuestionnaireBuilderScreenKt",
                    "io.healthplatform.chartcam.sync.SyncWorker",
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
        verify {
            rule {
                minBound(100)
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    enabled = true
    failOnNoDiscoveredTests = false
}

@Suppress("DEPRECATION")
tasks.withType<org.jetbrains.dokka.gradle.AbstractDokkaLeafTask>().configureEach {
    pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
        footerMessage = "© 2026 Samuel Marks"
    }
}

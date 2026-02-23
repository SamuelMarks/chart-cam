import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration

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
    androidTarget { 
        compilerOptions { 
            jvmTarget.set(JvmTarget.JVM_11) 
        } 
    } 
    
    listOf( 
        iosArm64(), 
        iosSimulatorArm64() 
    ).forEach { iosTarget ->
        iosTarget.binaries.framework { 
            baseName = "ChartCam" 
            isStatic = false
            freeCompilerArgs += listOf("-Xbinary=bundleId=io.healthplatform.chartcam.ChartCam")
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
            implementation(libs.androidx.security.crypto) 
            implementation(libs.sqldelight.android) 
            implementation(libs.sqlcipher.android)
            implementation(libs.androidx.camera.core) 
            implementation(libs.androidx.camera.camera2) 
            implementation(libs.androidx.camera.lifecycle) 
            implementation(libs.androidx.camera.view) 
        } 
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        commonMain.dependencies { 
            implementation(libs.google.fhir.model)
            implementation(libs.compose.runtime) 
            implementation(libs.compose.foundation) 
            implementation(libs.compose.material3) 
            implementation(compose.materialIconsExtended)

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
        val wasmJsMain by getting {
            dependencies {
                implementation(libs.sqldelight.webworker)
                implementation(npm("crypto-js", "4.2.0"))
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(libs.sqldelight.webworker)
                implementation(npm("crypto-js", "4.2.0"))

            }
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test) 
            implementation(libs.junit)
            implementation(libs.kotlinx.coroutines.test) 
            implementation(libs.ktor.client.mock)
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

android { 
    namespace = "io.healthplatform.chartcam" 
    compileSdk = libs.versions.android.compileSdk.get().toInt() 

    defaultConfig { 
        minSdk = libs.versions.android.minSdk.get().toInt()
        vectorDrawables.useSupportLibrary = true 
    } 
    packaging { 
        resources { 
            excludes += "/META-INF/{AL2.0,LGPL2.1}" 
        } 
    } 
    buildTypes { 
        getByName("release") { 
            isMinifyEnabled = false
        } 
    } 
    compileOptions { 
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    } 
} 

dependencies { 
    debugImplementation(libs.compose.uiTooling) 
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
        } 
    } 
}

kover {
    reports {
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

tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
        footerMessage = "© 2026 Samuel Marks"
    }
}

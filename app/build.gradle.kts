plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // kotlin("kapt") // Required for Room annotation processing - Temporarily disabled
}

android {
    namespace = "com.germanleraningwidget"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.germanleraningwidget"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "1.03"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // Production optimizations
            isDebuggable = false
            isPseudoLocalesEnabled = false
            isCrunchPngs = true
            
            // Build configuration fields
            buildConfigField("boolean", "ENABLE_LOGGING", "false")
            buildConfigField("String", "BUILD_TYPE", "\"release\"")
            
            // Optimize APK size and security
            packaging {
                resources {
                    excludes += listOf(
                        "META-INF/DEPENDENCIES",
                        "META-INF/LICENSE",
                        "META-INF/LICENSE.txt",
                        "META-INF/NOTICE",
                        "META-INF/NOTICE.txt",
                        "META-INF/INDEX.LIST",
                        "META-INF/MANIFEST.MF",
                        "META-INF/*.SF",
                        "META-INF/*.DSA",
                        "META-INF/*.RSA",
                        "**/*.kotlin_metadata",
                        "kotlin/**",
                        "DebugProbesKt.bin"
                    )
                }
            }
        }
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            
            // Debug configuration
            buildConfigField("boolean", "ENABLE_LOGGING", "true")
            buildConfigField("String", "BUILD_TYPE", "\"debug\"")
            
            // Debugging aids
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeKotlinCompilerExtension.get()
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    
    // Navigation
    implementation(libs.androidx.navigation.compose)
    
    // DataStore
    implementation(libs.androidx.datastore.preferences)
    
    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    
    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    
    // Material Icons
    implementation(libs.androidx.compose.material.icons.extended)
    
    // Material Design Components (for XML themes)
    implementation(libs.google.material)
    
    // Splash Screen
    implementation(libs.androidx.core.splashscreen)
    
    // Room Database (ENH-001: Database Architecture Migration) - Temporarily disabled for compatibility
    // implementation("androidx.room:room-runtime:2.5.2")
    // implementation("androidx.room:room-ktx:2.5.2")
    // kapt("androidx.room:room-compiler:2.5.2")
    
    // Core library desugaring for API level compatibility
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

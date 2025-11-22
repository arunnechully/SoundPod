import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.ksp)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("17")
        freeCompilerArgs.addAll("-Xcontext-receivers")
    }
}

android {
    namespace = "com.github.soundpod"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.github.soundpod"
        minSdk = 23
        targetSdk = 36
        versionCode = 13
        versionName = "1.0.4"
    }

    splits {
        abi {
            reset()
            isUniversalApk = true
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    sourceSets.all {
        kotlin.srcDir("src/$name/kotlin")
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.activity)
    implementation(libs.coil.compose)
    implementation(libs.coil.network)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.material3)
    implementation(libs.compose.navigation)
    implementation(libs.compose.shimmer)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.core.splashscreen)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.material.motion.compose)
    implementation(libs.media)
    implementation(libs.media3.exoplayer)
    implementation(libs.reorderable)
    implementation(libs.room)
    implementation(libs.swipe)
    implementation(libs.compose.lottie)
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    ksp(libs.room.compiler)
    implementation(projects.core.ui)
    implementation(projects.github)
    implementation(projects.innertube)
    implementation(projects.kugou)
    coreLibraryDesugaring(libs.desugaring)
}
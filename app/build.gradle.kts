import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.ksp)
}


kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

extensions.configure<ApplicationExtension>("android") {
    namespace = "com.github.soundpod"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.github.soundpod"
        minSdk = 23
        targetSdk = 37
        versionCode = 29
        versionName = "1.3.4-rc-1"
    }

    flavorDimensions += "store"

    productFlavors {
        create("fdroid") {
            dimension = "store"
            buildConfigField("boolean", "ENABLE_UPDATER", "false")
            proguardFiles("proguard-fdroid.pro")
        }
        create("github") {
            dimension = "store"
            buildConfigField("boolean", "ENABLE_UPDATER", "true")
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += setOf(
                "**/*.prof",
                "**/*.profi",
                "META-INF/version-control-info.textproto",
                "META-INF/com/android/build/gradle/app-metadata.properties",
                "META-INF/*.RSA",
                "META-INF/*.SF",
                "META-INF/*.DSA",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*"
            )
        }
    }

    splits {
        abi {
            reset()
            isUniversalApk = false
        }
    }

    buildTypes {

        debug {
            applicationIdSuffix = ".debug"
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true

            signingConfig = signingConfigs.getByName("debug")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isCrunchPngs = false
        }
    }

    sourceSets.all {
        kotlin.directories.add("src/$name/kotlin")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
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
    implementation(libs.compose.foundation)
    implementation(libs.material)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.material3)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.compose.navigation)
    implementation(libs.compose.shimmer)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.material.motion.compose)
    implementation(libs.media)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.reorderable)
    implementation(libs.room)
    implementation(libs.swipe)
    implementation(libs.core.splashscreen)
    implementation(libs.palette.ktx)
    implementation(libs.work.runtime.ktx)
    implementation(libs.preference.ktx)
    implementation(libs.compose.lottie)
    implementation(libs.datastore.preferences)
    implementation(libs.ui.geometry)
    implementation(libs.rhino)

    implementation(libs.kotlin.coroutines)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.serialization.json)

    ksp(libs.room.compiler)

    implementation(project(":core:ui"))
    implementation(project(":core:visuals"))
    implementation(project(":github"))
    implementation(project(":innertube"))
    implementation(project(":betterlyrics"))
    
    implementation(libs.timber)

    implementation(libs.newpipe.extractor)

    testImplementation(libs.junit)

    coreLibraryDesugaring(libs.desugaring)
}

tasks.configureEach {
    if (name.contains("fdroid", ignoreCase = true) && name.contains("ArtProfile", ignoreCase = true)) {
        enabled = false
    }
    // Disable resource shrinking for fdroid release to ensure reproducibility
    if (name == "shrinkFdroidReleaseRes") {
        enabled = false
    }
}

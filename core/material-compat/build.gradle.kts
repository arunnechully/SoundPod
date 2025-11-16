plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.soundpod.materialcompat"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }

    sourceSets.all {
        kotlin.srcDir("src/$name/kotlin")
    }

    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf("-Xcontext-receivers")
    }
}

dependencies {
    implementation(projects.core.ui)

    detektPlugins(libs.detekt.compose)
    detektPlugins(libs.detekt.formatting)
}

kotlin {
    jvmToolchain(21)
}


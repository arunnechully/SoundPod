import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.detekt)
}

extensions.configure<LibraryExtension>("android") {
    namespace = "com.soundpod.materialcompat"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
    }
}

androidComponents {
    onVariants { variant ->
        variant.sources.kotlin?.addStaticSourceDirectory("src/main/kotlin")
        variant.sources.kotlin?.addStaticSourceDirectory("src/${variant.name}/kotlin")
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

dependencies {
    implementation(projects.core.ui)
    detektPlugins(libs.detekt.compose)
    detektPlugins(libs.detekt.formatting)
}

kotlin {
    jvmToolchain(17)
}

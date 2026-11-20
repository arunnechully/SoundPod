import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.detekt)
    id("kotlin-parcelize")
}

extensions.configure<LibraryExtension>("android") {
    namespace = "com.github.soundcore"
    compileSdk = 37

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":innertube"))
    implementation(libs.core.ktx)
    implementation(libs.okhttp)
    implementation(libs.media3.exoplayer)
    implementation(libs.kotlin.coroutines)
}

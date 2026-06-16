plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":innertube"))
    implementation(libs.kotlin.coroutines)
}

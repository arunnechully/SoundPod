plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":innertube"))
    implementation(libs.kotlin.coroutines)
    implementation(libs.newpipe.extractor)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlin.serialization.json)
}

plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":innertube"))
    implementation(libs.kotlin.coroutines)
    compileOnly(libs.newpipe.extractor)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
}

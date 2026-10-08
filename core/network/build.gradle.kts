plugins {
    alias(libs.plugins.app.android.library)
    alias(libs.plugins.app.hilt)
    alias(libs.plugins.app.kotlin.serialization)
    alias(libs.plugins.app.detekt)
    alias(libs.plugins.app.spotless)
}

android {
    namespace = "io.github.gauthiercpx.roundtrip.core.network"
}

dependencies {
    api(project(":core:model"))
    api(libs.okhttp)
    implementation(libs.retrofit2)
    implementation(libs.retrofit2.kotlinx.serialization.converter)

    testImplementation(libs.bundles.unit.test)
    testImplementation(libs.mockwebserver)
}

plugins {
    alias(libs.plugins.app.android.library)
    alias(libs.plugins.app.hilt)
    alias(libs.plugins.app.kotlin.serialization)
    alias(libs.plugins.app.detekt)
    alias(libs.plugins.app.spotless)
}

android {
    namespace = "io.github.gauthiercpx.roundtrip.core.datastore"
}

dependencies {
    api(project(":core:domain"))
    api(project(":core:model"))
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.bundles.unit.test)
}

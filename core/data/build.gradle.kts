plugins {
    alias(libs.plugins.app.android.library)
    alias(libs.plugins.app.hilt)
    alias(libs.plugins.app.detekt)
    alias(libs.plugins.app.spotless)
}

android {
    namespace = "io.github.gauthiercpx.roundtrip.core.data"
}

dependencies {
    api(project(":core:datastore"))
    api(project(":core:domain"))
    api(project(":core:model"))
    api(project(":core:network"))

    testImplementation(libs.bundles.unit.test)
}

plugins {
    alias(libs.plugins.app.android.library)
    alias(libs.plugins.app.android.library.compose)
    alias(libs.plugins.app.detekt)
    alias(libs.plugins.app.spotless)
}

android {
    namespace = "io.github.gauthiercpx.roundtrip.core.ui"
}

dependencies {
    api(libs.bundles.compose)
    api(libs.androidx.glance.material3)
}

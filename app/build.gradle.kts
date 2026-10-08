plugins {
    alias(libs.plugins.app.android.application)
    alias(libs.plugins.app.android.application.compose)
    alias(libs.plugins.app.detekt)
    alias(libs.plugins.app.spotless)
}

android {
    namespace = "io.github.gauthiercpx.roundtrip"

    defaultConfig {
        applicationId = "io.github.gauthiercpx.roundtrip"
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.bundles.unit.test)
}

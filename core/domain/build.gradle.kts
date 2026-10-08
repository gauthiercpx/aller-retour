plugins {
    alias(libs.plugins.app.jvm.library)
    alias(libs.plugins.app.detekt)
    alias(libs.plugins.app.spotless)
}

dependencies {
    api(project(":core:model"))
}

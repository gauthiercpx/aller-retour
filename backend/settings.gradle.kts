rootProject.name = "round-trip-backend"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(":model", ":server")

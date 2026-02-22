pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "GetOnTrack"

include(":app")
include(":core")
include(":engine")
include(":policy")
include(":detector")
include(":system")
include(":overlay")
include(":storage")

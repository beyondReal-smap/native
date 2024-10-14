pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    val applicationVersion: String by settings
    val libraryVersion: String by settings
    val kotlinVersion: String by settings
    val navigationVersion: String by settings
    val hiltVersion: String by settings
    val googleServiceVersion: String by settings

    resolutionStrategy {
        eachPlugin {
            logger.quiet("requestId${requested.id.id}")
            when (requested.id.id) {
                "com.android.application" -> useVersion(applicationVersion)
                "com.android.library" -> useVersion(libraryVersion)
                "org.jetbrains.kotlin.android" -> useVersion(kotlinVersion)
                "androidx.navigation.safeargs.kotlin" -> useVersion(navigationVersion)
                "com.google.dagger.hilt.android" -> useVersion(hiltVersion)
                "com.google.gms.google-service" -> useVersion(googleServiceVersion)
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Smap"
include(":app")

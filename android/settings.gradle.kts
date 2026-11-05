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
        // The RemoteCompose player as androidx-main builds it (1.0.0-SNAPSHOT), from one pinned
        // androidx.dev build: gradle.properties' androidxSnapshotBuild. Only that group comes from it.
        val build = providers.gradleProperty("androidxSnapshotBuild").get()
        maven("https://androidx.dev/snapshots/builds/$build/artifacts/repository") {
            name = "androidxSnapshots"
            content { includeGroup("androidx.compose.remote") }
        }
    }
}

rootProject.name = "kobento-showcases"
include(":app")

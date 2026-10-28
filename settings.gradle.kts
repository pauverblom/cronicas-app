pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    // The Kotlin/Wasm plugin (web module) adds its own repositories to download Node.js, Yarn and Binaryen,
    // which FAIL_ON_PROJECT_REPOS would reject. The app and web modules declare none and use these.
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CronicasMarca"
include(":app")
include(":web")

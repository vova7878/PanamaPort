pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
rootProject.name = "PanamaPort"
include(":AndroidUnsafe", ":AndroidPanama", ":VarHandleApi", ":LLVM")
include(":stub_panama", ":stub_buffers", ":stub_invoke", ":stub_llvm")
 
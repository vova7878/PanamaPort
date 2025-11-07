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
rootProject.name = "PanamaPort"
include(":AndroidUnsafe", ":AndroidPanama", ":VarHandleApi", ":LLVM")
include(":stub_panama", ":stub_buffers", ":stub_invoke", ":stub_llvm")
 
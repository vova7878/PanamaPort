plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.v7878.foreign"
}
dependencies {
    api(project(":VarHandles"))
}


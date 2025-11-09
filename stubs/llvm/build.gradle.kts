plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.v7878.llvm"
}
dependencies {
    compileOnly(project(":stubs:panama"))
}


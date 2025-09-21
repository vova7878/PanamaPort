plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.v7878.llvm"
}

dependencies {
    compileOnly(project(":stub_panama"))
}

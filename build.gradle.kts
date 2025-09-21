plugins {
    alias(libs.plugins.android.library) apply false
}

subprojects {
    afterEvaluate {
        configure<com.android.build.api.dsl.LibraryExtension> {
            compileSdk = 36

            defaultConfig {
                minSdk = 26
            }
        }
        configure<JavaPluginExtension> {
            toolchain.languageVersion = JavaLanguageVersion.of(21)
        }
    }
}

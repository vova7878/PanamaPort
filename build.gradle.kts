plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.maven.publish) apply false
}

subprojects {
    afterEvaluate {
        if (plugins.hasPlugin("com.android.library")) {
            configure<com.android.build.api.dsl.LibraryExtension> {
                enableKotlin = false

                compileSdk {
                    version = release(36)
                }

                defaultConfig {
                    minSdk = 26
                }
            }
            configure<JavaPluginExtension> {
                toolchain.languageVersion = JavaLanguageVersion.of(21)
            }
        }
    }
}

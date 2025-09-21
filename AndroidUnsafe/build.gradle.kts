plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.raung)
}

android {
    namespace = "com.v7878.unsafe"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        buildConfig = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    compileOnly(project(":stub_buffers"))
    compileOnly(project(":stub_panama"))
    compileOnly(project(":stub_llvm"))

    api(libs.dexfile)

    implementation(libs.r8.annotations)

    implementation(libs.sun.unsafewrapper)

    //TODO: runtimeOnlyApi?
    runtimeOnly(project(":AndroidPanama"))
    runtimeOnly(project(":LLVM"))
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}

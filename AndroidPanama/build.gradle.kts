plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "com.v7878.foreign"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    compileOnly(project(":stub_llvm"))
    api(project(":VarHandleApi"))

    implementation(project(":AndroidUnsafe"))

    implementation(libs.sun.cleanerstub)
    implementation(libs.r8.annotations)
    implementation(libs.dexfile)
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

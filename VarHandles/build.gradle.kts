plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "com.v7878.invoke"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    publishing {
        multipleVariants {
            includeBuildTypeValues("debug", "release")
            withSourcesJar()
        }
    }
}

dependencies {
    compileOnly(project(":stubs:invoke"))

    implementation(libs.r8.annotations)

    runtimeOnly(project(":Unsafe"))
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = false)
    signAllPublications()

    coordinates(
        groupId = "io.github.vova7878.panama",
        artifactId = "VarHandles",
        version = project.version.toString()
    )

    pom {
        name.set("PanamaPort-VarHandles")
        description.set("Implementation of FFM API for Android 8.0+")
        inceptionYear.set("2025")
        url.set("https://github.com/vova7878/PanamaPort")

        licenses {
            license {
                name.set("MIT AND GPL-2.0-with-classpath-exception")
                //TODO: url.set("https://github.com/vova7878/PanamaPort#licensing")
                distribution.set("repository")
                comments.set(
                    """
                    This artifact is licensed under both MIT and GPL-2.0 with Classpath Exception concurrently.
                    - MIT applies to all parts of this project except for the code in `Core/src/openjdk` folder.
                    - GPL-2.0 with Classpath Exception applies to code derived from OpenJDK in `Core/src/openjdk`.
                    """.trimIndent()
                )
            }
        }

        developers {
            developer {
                id.set("vova7878")
                name.set("Vladimir Kozelkov")
                url.set("https://github.com/vova7878")
            }
        }

        scm {
            url.set("https://github.com/vova7878/PanamaPort")
            connection.set("scm:git:git://github.com/vova7878/PanamaPort.git")
            developerConnection.set("scm:git:ssh://git@github.com/vova7878/PanamaPort.git")
        }
    }
}

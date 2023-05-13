import com.android.build.api.dsl.CommonExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.diffplug.spotless") version "6.18.0" apply false
    id("org.gradle.android.cache-fix") version "2.7.1" apply false
    id("com.google.devtools.ksp") version "1.8.21-1.0.11" apply false
}

buildscript {
    extra["compileSdkVersion"] = 33
    extra["targetSdkVersion"] = 33
    extra["minSdkVersion"] = 23

    val jacocoEnabled: String? by project
    extra["jacocoEnabled"] = jacocoEnabled?.toBoolean() ?: false

    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath(libs.android.gradle.plugin)
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.hilt.gradle.plugin)
    }
}

subprojects {
    afterEvaluate {
        plugins.withType<com.android.build.gradle.api.AndroidBasePlugin> {
            apply(plugin = "org.gradle.android.cache-fix")
        }

        apply(plugin = "com.diffplug.spotless")
        extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
            kotlin {
                target("**/*.kt")
                targetExclude("**/build/**/*.kt")

                ktlint()
                    .userData(mapOf("android" to "true"))
            }
            kotlinGradle {
                target("**/*.kts")
                targetExclude("**/build/**/*.kts")

                ktlint()
            }
            format("misc") {
                target("*.gradle", "*.md", ".gitignore")

                trimTrailingWhitespace()
                indentWithSpaces()
                endWithNewline()
            }
        }

        extensions.findByType(CommonExtension::class.java)?.apply {
            compileOptions {
                sourceCompatibility(JavaVersion.VERSION_17)
                targetCompatibility(JavaVersion.VERSION_17)
            }

            testOptions {
                animationsDisabled = true

                unitTests {
                    isReturnDefaultValues = true
                    isIncludeAndroidResources = true

                    all {
                        it.useJUnitPlatform()
                    }
                }
            }
        }

        tasks.withType<KotlinCompile>().configureEach {
            kotlinOptions.apply {
                jvmTarget = "17"
                freeCompilerArgs = buildList {
                    addAll(freeCompilerArgs)

                    add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")

                    if (project.findProperty("composeCompilerReports") == "true") {
                        val composeCompilerPath = "${project.buildDir.absolutePath}/compose_compiler"
                        add("-P")
                        add("plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=$composeCompilerPath")
                        add("-P")
                        add("plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$composeCompilerPath")
                    }
                }
            }
        }

        tasks.findByName("preBuild")?.dependsOn("spotlessCheck")
    }
}

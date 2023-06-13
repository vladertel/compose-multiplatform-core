import androidx.build.AndroidXComposePlugin
import java.util.Properties

plugins {
    id("AndroidXPlugin")
    id("AndroidXComposePlugin")
    id("kotlin-multiplatform")
    id("org.jetbrains.gradle.apple.applePlugin") version "222.4550-0.21"
}

val runOnDevice = findProperty("xcode.arch") == "arm64"
val isArm64Host = System.getProperty("os.arch") == "aarch64"

AndroidXComposePlugin.applyAndConfigureKotlinPlugin(project)


dependencies {

}

repositories {
    mavenLocal()
}

kotlin {
    val additionalCompilerArgs = listOf(
        "-linker-option", "-framework", "-linker-option", "Metal",
        "-linker-option", "-framework", "-linker-option", "CoreText",
        "-linker-option", "-framework", "-linker-option", "CoreGraphics"
    )
    if (runOnDevice) {
        ios("uikitArm64") {
            binaries {
                framework {
                    baseName = "shared"
                    freeCompilerArgs += additionalCompilerArgs
                }
            }
        }
    } else {
        if (isArm64Host) {
            iosSimulatorArm64("uikitSimArm64") {
                binaries {
                    framework {
                        baseName = "shared"
                        freeCompilerArgs += additionalCompilerArgs
                    }
                }
            }
        } else {
            ios() {
                binaries {
                    framework {
                        baseName = "shared"
                        freeCompilerArgs += additionalCompilerArgs
                    }
                }
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":compose:foundation:foundation"))
                implementation(project(":compose:foundation:foundation-layout"))
                implementation(project(":compose:material:material"))
                implementation(project(":compose:mpp"))
                implementation(project(":compose:mpp:demo"))
                implementation(project(":compose:runtime:runtime"))
                implementation(project(":compose:ui:ui"))
                implementation(project(":compose:ui:ui-graphics"))
                implementation(project(":compose:ui:ui-text"))
                implementation(libs.kotlinCoroutinesCore)
            }
        }
        val skikoMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.skikoCommon)
            }
        }
        val nativeMain by creating { dependsOn(skikoMain) }
        val darwinMain by creating { dependsOn(nativeMain) }
        val uikitMain by creating { dependsOn(darwinMain) }
        if (runOnDevice) {
            val uikitArm64Main by getting { dependsOn(uikitMain) }
        } else {
            if (isArm64Host) {
                val uikitSimArm64Main by getting { dependsOn(uikitMain) }
            } else {
                val iosMain by getting { dependsOn(uikitMain) }
            }
        }
    }
}

apple {
    iosApp {
        productName = "composeuikit"

        sceneDelegateClass = "SceneDelegate"
        launchStoryboard = "LaunchScreen"

        val projectProperties = Properties()
        val projectPropertiesFile = rootProject.file("project.properties")
        if (projectPropertiesFile.exists()) {
            projectProperties.load(projectPropertiesFile.reader())
            val teamId = projectProperties.getProperty("TEAM_ID")
            if (teamId != null) {
                buildSettings.DEVELOPMENT_TEAM(teamId)
            }
        }
        buildSettings.DEPLOYMENT_TARGET("15.0")

        // TODO: add 'CADisableMinimumFrameDurationOnPhone' set to 'YES'

        dependencies {
            // Here we can add additional dependencies to Swift sourceSet
        }
    }
}

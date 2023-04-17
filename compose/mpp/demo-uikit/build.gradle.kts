import androidx.build.AndroidXComposePlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("AndroidXPlugin")
    id("AndroidXComposePlugin")
    id("kotlin-multiplatform")
    id("org.jetbrains.gradle.apple.applePlugin") version "222.4550-0.21"
}

// Value of the TEAM_ID like in Config.xcconfig
// https://github.com/JetBrains/compose-multiplatform-template#running-on-a-real-ios-device
val developmentTeamToRunOnRealDevice = "JMS9FA69HB"
val runOnDevice = developmentTeamToRunOnRealDevice.isNotEmpty()

AndroidXComposePlugin.applyAndConfigureKotlinPlugin(project)

dependencies {

}

repositories {
    mavenLocal()
}

kotlin {
    if (runOnDevice) {
        ios("uikitArm64") {
            binaries {
                framework {
                    baseName = "shared"
                    freeCompilerArgs += listOf(
                        "-linker-option", "-framework", "-linker-option", "Metal",
                        "-linker-option", "-framework", "-linker-option", "CoreText",
                        "-linker-option", "-framework", "-linker-option", "CoreGraphics"
                    )
                }
            }
        }
    } else {
        if (System.getProperty("os.arch") == "aarch64") {
            iosSimulatorArm64("uikitSimArm64") {
                binaries {
                    framework {
                        baseName = "shared"
                        freeCompilerArgs += listOf(
                            "-linker-option", "-framework", "-linker-option", "Metal",
                            "-linker-option", "-framework", "-linker-option", "CoreText",
                            "-linker-option", "-framework", "-linker-option", "CoreGraphics"
                        )
                    }
                }
            }
        } else {
            iosX64("uikitX64") {
                binaries {
                    framework {
                        baseName = "shared"
                        freeCompilerArgs += listOf(
                            "-linker-option", "-framework", "-linker-option", "Metal",
                            "-linker-option", "-framework", "-linker-option", "CoreText",
                            "-linker-option", "-framework", "-linker-option", "CoreGraphics"
                        )
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
            if (System.getProperty("os.arch") == "aarch64") {
                val uikitSimArm64Main by getting { dependsOn(uikitMain) }
            } else {
                val uikitX64Main by getting { dependsOn(uikitMain) }
            }
        }
    }
}

apple {
    iosApp {
        productName = "composeuikit"

        sceneDelegateClass = "SceneDelegate"
        launchStoryboard = "LaunchScreen"

        buildSettings.DEVELOPMENT_TEAM(developmentTeamToRunOnRealDevice)
        buildSettings.DEPLOYMENT_TARGET("15.0")

        dependencies {
            // Here we can add additional dependencies to Swift sourceSet
        }
    }
}

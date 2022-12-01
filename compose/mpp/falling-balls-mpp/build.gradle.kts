//import org.jetbrains.compose.compose
//import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import androidx.build.AndroidXComposePlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("AndroidXPlugin")
    id("AndroidXComposePlugin")
    id("kotlin-multiplatform")
    id("application")
}

AndroidXComposePlugin.applyAndConfigureKotlinPlugin(project)

dependencies {

}

val resourcesDir = "$buildDir/resources"
val skikoWasm by configurations.creating

dependencies {
    skikoWasm(libs.skikoWasm)
}

val unzipTask = tasks.register("unzipWasm", Copy::class) {
    destinationDir = file(resourcesDir)
    from(skikoWasm.map { zipTree(it) })
}

repositories {
    mavenLocal()
}


kotlin {
//    jvm("desktop")
    js(IR) {
        browser()
        binaries.executable()
    }
    wasm() {
        browser {

        }
        binaries.executable()
    }


    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":compose:animation:animation"))
                implementation(project(":compose:animation:animation-core"))
                implementation(project(":compose:foundation:foundation"))
                implementation(project(":compose:foundation:foundation-layout"))
                implementation(project(":compose:material:material"))
                implementation(project(":compose:runtime:runtime"))
                implementation(project(":compose:ui:ui"))
                implementation(project(":compose:ui:ui-graphics"))
                implementation(project(":compose:ui:ui-text"))
                implementation(libs.kotlinCoroutinesCore)
            }
        }

//        val commonTest by getting {
//            dependencies {
//                implementation(kotlin("test-common"))
//                implementation(kotlin("test-annotations-common"))
//            }
//        }

        val skikoMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.skikoCommon)
            }
        }


//        val desktopMain by getting {
//             dependencies {
//                implementation(compose.desktop.currentOs)
//             }
//        }
//
        val jsMain by getting {
            dependsOn(skikoMain)
            resources.setSrcDirs(resources.srcDirs)
            resources.srcDirs(unzipTask.map { it.destinationDir })
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }

        val wasmMain by getting {
            dependsOn(skikoMain)
            resources.setSrcDirs(resources.srcDirs)
            resources.srcDirs(unzipTask.map { it.destinationDir })
            dependencies {
                implementation(kotlin("stdlib-wasm"))
            }
        }
//        val nativeMain by creating {
//            dependsOn(commonMain)
//        }
//        val macosMain by creating {
//            dependsOn(nativeMain)
//        }
//        val macosX64Main by getting {
//            dependsOn(macosMain)
//        }
//        val macosArm64Main by getting {
//            dependsOn(macosMain)
//        }
//        val uikitMain by creating {
//            dependsOn(nativeMain)
//        }
//        val uikitX64Main by getting {
//            dependsOn(uikitMain)
//        }
//        val uikitArm64Main by getting {
//            dependsOn(uikitMain)
//        }
    }
}

//compose.desktop {
//    application {
//        mainClass = "Main_desktopKt"
//
//        nativeDistributions {
//            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
//            packageName = "Falling Balls"
//            packageVersion = "1.0.0"
//
//            windows {
//                menuGroup = "Compose Examples"
//                // see https://wixtoolset.org/documentation/manual/v3/howtos/general/generate_guids.html
//                upgradeUuid = "18159995-d967-4CD2-8885-77BFA97CFA9F"
//            }
//        }
//    }
//}

//compose.experimental {
//    web.application {}
////    uikit.application {}
//}

//tasks.withType<KotlinCompile> {
//    kotlinOptions.jvmTarget = "11"
//}

//kotlin {
//    targets.withType<KotlinNativeTarget> {
//        binaries.all {
//            // TODO: the current compose binary surprises LLVM, so disable checks for now.
//            freeCompilerArgs += "-Xdisable-phases=VerifyBitcode"
//        }
//    }
//}

// a temporary workaround for a bug in jsRun invocation - see https://youtrack.jetbrains.com/issue/KT-48273
//afterEvaluate {
//    rootProject.extensions.configure<NodeJsRootExtension> {
//        versions.webpackDevServer.version = "4.0.0"
//        versions.webpackCli.version = "4.9.0"
//        nodeVersion = "16.0.0"
//    }
//}


project.tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += listOf(
//        "-Xklib-enable-signature-clash-checks=false",
        //"-Xplugin=${project.properties["compose.plugin.path"]}",
        "-Xir-dce"
    )
}
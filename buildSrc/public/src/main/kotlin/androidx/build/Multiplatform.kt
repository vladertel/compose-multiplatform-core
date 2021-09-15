/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.build

import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation

/**
 * Setting this property enables multiplatform builds of Compose
 */
const val COMPOSE_MPP_ENABLED = "androidx.compose.multiplatformEnabled"

/**
 * Publishing layout setup for the open-expect-lite mode.
 *
 */
const val COMPOSE_MPP_OPEN_EXPECT_LITE_MODE = "androidx.compose.openExpectLiteMode"

enum class OpenExpectLiteMode {
    ANDROIDX, ORG_JETBRAINS
}

/**
 * Setting this property enables JS compiler tests of Compose
 */
const val COMPOSE_JS_COMPILER_TESTS_ENABLED = "androidx.compose.jsCompilerTestsEnabled"

class Multiplatform {
    companion object {
        fun Project.isMultiplatformEnabled(): Boolean {
            return properties.get(COMPOSE_MPP_ENABLED)?.toString()?.toBoolean() ?: false
        }

        fun Project.isJsCompilerTestsEnabled(): Boolean {
            return properties.get(COMPOSE_JS_COMPILER_TESTS_ENABLED)?.toString()?.toBoolean() ?: false
        }

        fun setEnabledForProject(project: Project, enabled: Boolean) {
            project.extra.set(COMPOSE_MPP_ENABLED, enabled)
        }

        fun Project.openExpectLiteMode(): OpenExpectLiteMode? =
            findProperty(COMPOSE_MPP_OPEN_EXPECT_LITE_MODE)?.let { property ->
                OpenExpectLiteMode.values().find { it.name.equals("$property", true) }
            }
    }
}

object MultiplatformUtils {
    @JvmStatic
    fun isOpenExpectLiteAndroidx(project: Project): Boolean =
        with(Multiplatform) { project.openExpectLiteMode() == OpenExpectLiteMode.ANDROIDX }

    @JvmStatic
    fun isOpenExpectLiteOrgJetbrains(project: Project): Boolean =
        with(Multiplatform) { project.openExpectLiteMode() == OpenExpectLiteMode.ORG_JETBRAINS }

    @JvmStatic
    fun disableCompilationsOfTarget(target: KotlinTarget) {
        target.compilations.all { compilation ->
            val tasksToDisable = listOfNotNull(
                compilation.compileKotlinTask,
                (compilation as? KotlinJvmAndroidCompilation)?.androidVariant
                    ?.javaCompileProvider?.get()
            )
            tasksToDisable.forEach { taskToDisable ->
                taskToDisable.enabled = false
                val cleanTask =
                    target.project.tasks.named("clean" + taskToDisable.name.replaceFirstChar { it.uppercase() })
                taskToDisable.dependsOn(cleanTask)
            }
            compilation.output.classesDirs.setFrom(emptyList<Any>())

            if (compilation is KotlinJvmAndroidCompilation) {
                @Suppress("deprecation")
                (compilation.androidVariant as? com.android.build.gradle.api.LibraryVariant)
                    ?.packageLibraryProvider
                    ?.configure { aarTask ->
                        aarTask.exclude("**/*.jar")
                        aarTask.exclude("**/*.txt")
                        aarTask.exclude("**/*.xml")
                    }
            }
        }
    }
}

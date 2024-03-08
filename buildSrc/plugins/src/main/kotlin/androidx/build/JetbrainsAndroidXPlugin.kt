/*
 * Copyright 2024 The Android Open Source Project
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

@file:Suppress("unused")

package androidx.build

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.KonanTarget

@Suppress("UNUSED_PARAMETER")
open class JetbrainsExtensions(
    val project: Project,
    val multiplatformExtension: KotlinMultiplatformExtension
) {

    val defaultKonanTargetsPublishedToAndroidx = setOf(
        KonanTarget.LINUX_X64,
        KonanTarget.IOS_X64,
        KonanTarget.IOS_ARM64,
        KonanTarget.IOS_SIMULATOR_ARM64,
        KonanTarget.MACOS_X64,
        KonanTarget.MACOS_ARM64,
    )

    @JvmOverloads
    fun configureKNativeRedirectingDependenciesInKlibManifest(
        konanTargets: Set<KonanTarget> = defaultKonanTargetsPublishedToAndroidx
    ) {
        multiplatformExtension.targets.all {
            if (it is KotlinNativeTarget && it.konanTarget in konanTargets) {
                it.substituteForOelPublishedDependencies()
            }
        }
    }

    /**
     * K/Native stores the dependencies in klib manifest and tries to resolve them during compilation.
     * Since we use project dependency - implementation(project(...)), the klib manifest will reference
     * our groupId (for example org.jetbrains.compose.collection-internal instead of androidx.collection).
     * Therefore, the dependency can't be resolved since we don't publish libs for some k/native targets.
     *
     * To fix that, we need to make sure
     * that the project dependency is substituted by a module dependency (from androidx).
     * We do this here. It should be called only for appropriate k/native targets.
     *
     * For available androidx targets see:
     * https://maven.google.com/web/index.html#androidx.annotation
     * https://maven.google.com/web/index.html#androidx.collection
     */
    fun KotlinNativeTarget.substituteForOelPublishedDependencies() {
        val comp = compilations.getByName("main")
        val androidAnnotationVersion =
            project.findProperty("artifactRedirecting.androidx.annotation.version")!!
        val androidCollectionVersion =
            project.findProperty("artifactRedirecting.androidx.collection.version")!!
        val androidLifecycleVersion =
            project.findProperty("artifactRedirecting.androidx.lifecycle.version")!!
        listOf(
            comp.configurations.compileDependencyConfiguration,
            comp.configurations.runtimeDependencyConfiguration,
            comp.configurations.apiConfiguration,
            comp.configurations.implementationConfiguration,
            comp.configurations.runtimeOnlyConfiguration,
            comp.configurations.compileOnlyConfiguration,
        ).forEach { c ->
            c?.resolutionStrategy {
                it.dependencySubstitution {
                    it.substitute(it.project(":annotation:annotation"))
                        .using(it.module("androidx.annotation:annotation:$androidAnnotationVersion"))
                    it.substitute(it.project(":collection:collection"))
                        .using(it.module("androidx.collection:collection:$androidCollectionVersion"))
                    it.substitute(it.project(":lifecycle:lifecycle-common"))
                        .using(it.module("androidx.lifecycle:lifecycle-common:$androidLifecycleVersion"))
                    it.substitute(it.project(":lifecycle:lifecycle-runtime"))
                        .using(it.module("androidx.lifecycle:lifecycle-runtime:$androidLifecycleVersion"))
                }
            }
        }
    }

}
class JetbrainsAndroidXPlugin : Plugin<Project> {

    @Suppress("UNREACHABLE_CODE", "UNUSED_VARIABLE")
    override fun apply(project: Project) {
        // we need KotlinMultiplatform Plugin to be applied first. See applyAndConfigure below.
    }

    companion object {

        @Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE")
        @JvmStatic
        // this should be called only after KotlinMultiplatform plugin was applied
        fun applyAndConfigure(
            project: Project
        ) {
            val multiplatformExtension =
                project.extensions.getByType(KotlinMultiplatformExtension::class.java)

            val extension = project.extensions.create<JetbrainsExtensions>(
                "jetbrainsExtension",
                project,
                multiplatformExtension
            )

            // Note: Currently we call it unconditionally since Androidx provides the same set of
            // Konan targets for all multiplatform libs they publish.
            // In the future we might need to call it with non-default konan targets set in some modules
            extension.configureKNativeRedirectingDependenciesInKlibManifest()
        }
    }
}

/*
 * Copyright 2023 The Android Open Source Project
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

val knownNonComposeFolders = listOf(
    "busytown",
    "development",
    "docs",
    "docs-public",
    "docs-tip-of-tree",
    "fakeannotations",
    "frameworks",
    "playground-common",
    "samples",
)

project.tasks.create("logNonComposeFolders").doLast {
    val outputFile = File(rootDir, "nonComposeFolders").also {
        it.writeText("")
    }
    val composeModules = readModuleInfo(File(rootDir, "composeModules"))
    val nonComposeModules = readModuleInfo(File(rootDir, "nonComposeModules"))
    logNonComposeFolders(
        rootDir,
        composeModules.map { it.file(rootDir) },
        nonComposeModules.map { it.file(rootDir) },
        outputFile
    )
    logKnownNonComposeFolders(rootDir, outputFile)
}

fun logKnownNonComposeFolders(rootDir: File, outputFile: File) {
    for (knownFolder in knownNonComposeFolders) {
        val file = File(rootDir, knownFolder)
        outputFile.appendText(file.absolutePath + "\n")
    }
}

fun logNonComposeFolders(
    file: File,
    composeModules: List<File>,
    nonComposeModules: List<File>,
    outputFile: File
) {
    if (!file.isDirectory) {
        return
    }
    for (child in file.listFiles()!!) {
        val haveComposeModules = composeModules.any { child.contains(it) }
        val haveNonComposeModules = nonComposeModules.any { child.contains(it) }
        if (haveNonComposeModules && !haveComposeModules) {
            outputFile.appendText(child.absolutePath + "\n")
        }
        if (haveComposeModules && haveNonComposeModules) {
            logNonComposeFolders(child, composeModules, nonComposeModules, outputFile)
        }
    }
}

fun File.contains(file: File): Boolean {
    if (this == file) {
        return true
    }
    var parent = file.parentFile
    while (parent != null) {
        if (parent == this) {
            return true
        }
        parent = parent.parentFile
    }
    return false
}

fun readModuleInfo(file: File): List<ModuleInfo> {
    val rawModules = file.readText().trim().split("\n")
    return rawModules.map {
        val (moduleId, pathRaw) = it.split(' ')
        val path: String? = if (pathRaw != "null") {
            pathRaw
        } else {
            null
        }
        ModuleInfo(moduleId, path)
    }
}

class ModuleInfo(moduleId: String, path: String?) {
    private val realPath: String = path ?: run {
        moduleId.trimStart(':').replace(":", "/")
    }

    fun file(rootDir: File): File {
        return File(rootDir, realPath)
    }
}
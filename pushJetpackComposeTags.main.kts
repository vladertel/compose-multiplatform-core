/**
 * Run from command line:
 * 1. Download https://github.com/JetBrains/kotlin/releases/tag/v1.9.22 and add `bin` to PATH
 * 2. Call `kotlin <fileName>`
 *
 * Run from IntelliJ:
 * 1. Right click on the script
 * 2. More Run/Debug
 * 3. Modify Run Configuration...
 * 4. Clear all "Before launch" tasks (you can edit the system-wide template as well)
 * 5. OK
 */

@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("org.jsoup:jsoup:1.17.2")

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

val libs = listOf(
    "https://developer.android.com/jetpack/androidx/releases/compose-material3#version_12_2",
)

fun tagName(libName: String, version: String) = "androidx/$libName/$version"
fun tagNamePattern(libName: String) = "androidx/$libName/*"

"git remote add aosp https://android.googlesource.com/platform/frameworks/support".execCommand().apply {
    if (this.isNullOrEmpty()) {
        println("Added aosp https://android.googlesource.com/platform/frameworks/support repo")
    } else {
        check(contains("already exists"))
    }
}
"git fetch aosp".runCommand()

for (lib in libs) {
    println("=== Loading $lib")

    // compose-material3 from ../releases/compose-material3#version_12_2
    val libName = lib.substringBefore("#").substringAfterLast("/").validateLibName()
    // version_12_2 from ../releases/compose-material3#version_12_2
    val id = lib.substringAfter("#")

    fun Element.extractCommitAndVersion(): Pair<String, String> {
        val commit = attr("href").substringAfter("..").substringBefore("/").validateCommit()
        val version = text().substringAfter("Version ").substringBefore(" contains").validateVersion()
        return commit to version
    }

    Jsoup
        .connect(lib)
        .get()
        .select("*")
        .asSequence()
        .dropWhile { it.id() != id }
        .drop(1)
        .takeWhile { it.tagName() != "h2" }
        .filter { it.tagName() == "a" && it.text().contains("contains these commits") }
        .map { it.extractCommitAndVersion() }
        .forEach { (commit, version) ->
            val tagName = tagName(libName, version)
            println("Creating tag $tagName on $commit")
            "git tag -f $tagName $commit".runCommand()
        }

    println("Pushing tags")
    "git push origin tag ${tagNamePattern(libName)}".runCommand()
}

fun String.validateCommit() = apply {
    check(isNotEmpty() && all { it.isDigit() || it.isLetter() }) {
        "Commit name isn't correct: $this"
    }
}

fun String.validateVersion() = apply {
    check(isNotEmpty() && all { it.isDigit() || it.isLetter() || it == '.' || it == '-' }) {
        "Version name isn't correct: $this"
    }
}

fun String.validateLibName() = apply {
    check(isNotEmpty() && all { it.isDigit() || it.isLetter() || it == '.' || it == '-' }) {
        "Lib name isn't correct: $this"
    }
}

// from https://stackoverflow.com/a/41495542
fun String.runCommand(workingDir: File = File(".")) {
    ProcessBuilder(*split(" ").toTypedArray())
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
        .waitFor(5, TimeUnit.MINUTES)
}

fun String.execCommand(workingDir: File = File(".")): String? {
    try {
        val parts = this.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        proc.waitFor(60, TimeUnit.MINUTES)
        return proc.inputStream.bufferedReader().readText()
    } catch(e: IOException) {
        e.printStackTrace()
        return null
    }
}
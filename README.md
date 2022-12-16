# Fork of https://github.com/androidx/androidx (androidx-main branch) to test Compose Compiler

TODO(igor.demin@jetbrains.com): we need to test Compose Compiler for JS and Native too

## Developing in IDE

1. Download Android Studio version that is specified in [libs.versions.toml](gradle/libs.versions.toml#L11)

2. Open the project in Android Studio

3. Make sure that you use OpenJDK 11 in `Project settings -> Project`, and have `Gradle JVM = Project SDK` in `Settings -> Build, Executions, Deployment -> Build Tools -> Gradle`

4. Change Kotlin version in `gradle/libs.versions.toml` if needed (search for `kotlin = `)

5. Run `All tests`, `Compiler (Unit tests)`, `Runtime (Unit tests)` or `Runtime (Android integration tests)` to run tests.
If you don't see them, restart Android Studio (it seems a bug).

Notes:
- To run All tests you need a connected Android device or emulator (Android studio don't run it automatically)
- Compiler tests seems don't work on Windows

## Gradle commands

1. Gradle command to build Compose Compiler:
```
export COMPOSE_CUSTOM_VERSION=0.0.0-custom2
./gradlew compose:compiler:compiler:publishMavenPublicationToLocalRepository
```
The artifact `androidx.compose.compiler:compiler:0.0.0-custom2` will be in `~/.m2/repository/androidx/compose/compiler/compiler/0.0.0-custom2`

2. Gradle command to test Compose Runtime:
```
./gradlew :compose:runtime:runtime:testDebugUnitTest :compose:runtime:runtime:integration-tests:connectedAndroidTest :compose:compiler:compiler-hosted:integration-tests:testDebugUnitTest
```
It runs unit tests and Android tests (there should be a connected Android device or emulator)

# WebFlux with Kotlin by Example

A set of examples on how to use Spring's WebFlux framework with Kotlin, with an emphasis on reactive and asynchronous aspects.

## Bootstrapping a project

Go to [https://start.spring.io](https://start.spring.io), and select:
- Project: Gradle project
- Language: Kotlin
- Spring Boot: most recent stable release
- Packaging: Jar
- Java: 11
- Dependencies: Spring Reactive Web

When selecting "Generate", a `zip` archive is created containing the project basic structure:
- `gradle` folder, `gradlew` file, and `gradlew.bat` file, containing the gradle wrapper and bootstrapper for a given version (e.g. `6.0.1`).
- `build.gradle.kts` file with the Gradle build script. Notice that this build script uses the Kotlin language and not the Gradle language.
This choice was done by the project generator because the selected language was Kotlin.
All the required dependencies needed are defined inside this file.
- `settings.gradle.kts` file with additional Gradle settings.
- `src` folder containing two source files, one in `main` and another in `test`:
  - A file with the application entry point.
  - A test file with a simple test that confirms the Spring context can be correctly initialized.

### Application entry point

Notice that the application entry point is a _global_ kotlin function.
```
fun main(args: Array<String>) {
    runApplication<App>(*args)
}
```

Also, `runApplication` is a top level function provided by Spring Boot ([source](https://github.com/spring-projects/spring-boot/blob/master/spring-boot-project/spring-boot/src/main/kotlin/org/springframework/boot/SpringApplicationExtensions.kt#L29)
that takes advantage of [reified generics](https://kotlinlang.org/docs/reference/inline-functions.html) to avoid passing a `Class` object.

Spring Boot requires a correctly annotated _application_ class to be passed to `runApplication`, so an empty class is used for that purpose.
```
@SpringBootApplication
class App
```
This class can be used to contain _bean_ definitions.





import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-test-fixtures`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

// Lets `./gradlew testDebugUnitTest` also run this JVM module's tests.
tasks.register("testDebugUnitTest") {
    group = "verification"
    description = "Runs the unit tests (alias of `test` for this JVM module)."
    dependsOn(tasks.named("test"))
}

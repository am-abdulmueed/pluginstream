plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.android.multiplatform.library) apply false
    alias(libs.plugins.buildkonfig) apply false // Universal build config
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false

    // Flow plugins
    id("com.google.dagger.hilt.android") version "2.59" apply false
    id("com.google.devtools.ksp") version "2.3.7" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20" apply false
}

allprojects {
    // https://docs.gradle.org/current/userguide/upgrading_major_version_9.html#test_task_fails_when_no_tests_are_discovered
    tasks.withType<AbstractTestTask>().configureEach {
        failOnNoDiscoveredTests = false
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10" apply false
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://artifacts.objectbox.io/maven") }
    }
    dependencies {
        // 플러그인을 찾는 가장 확실한 방법입니다.
        classpath("io.objectbox:objectbox-gradle-plugin:4.0.0")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.secrets) apply false
}
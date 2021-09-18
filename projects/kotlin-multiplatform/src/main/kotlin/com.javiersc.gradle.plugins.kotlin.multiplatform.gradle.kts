import com.javiersc.plugins.android.core.AndroidSdk
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    sourceSets {
        all {
            defaultLanguageSettings
            kotlin.srcDirs("$name/kotlin")
            resources.srcDirs("$name/resources")
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
        sourceCompatibility = JavaVersion.VERSION_11.toString()
        targetCompatibility = JavaVersion.VERSION_11.toString()
    }
}

android {
    compileSdk = AndroidSdk.compileSdk

    defaultConfig { minSdk = AndroidSdk.minSdk }

    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_11)
        targetCompatibility(JavaVersion.VERSION_11)
    }

    sourceSets.all { manifest.srcFile("android${name.capitalize()}/AndroidManifest.xml") }
}
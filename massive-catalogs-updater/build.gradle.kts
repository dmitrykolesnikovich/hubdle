plugins {
    `kotlin-dsl`
    publish
    `plugin-publish`
    `accessors-generator`
}

pluginBundle {
    tags =
        listOf(
            "gradle",
            "massive catalogs",
            "versions catalogs",
        )
}

gradlePlugin {
    plugins {
        named("com.javiersc.gradle.plugins.massive.catalogs.updater") {
            id = "com.javiersc.gradle.plugins.massive.catalogs.updater"
            displayName = "Massive Catalogs Updater"
            description = "A plugin for updating Massive Catalogs"
        }
    }
}

dependencies {
    api(projects.pluginAccessors)
    api(projects.core)

    implementation(libs.jsoup.jsoup)
    implementation(libs.javiersc.semanticVersioning.semanticVersioningCore)

    implementation(pluginLibs.jetbrains.kotlin.kotlinGradlePlugin)
}
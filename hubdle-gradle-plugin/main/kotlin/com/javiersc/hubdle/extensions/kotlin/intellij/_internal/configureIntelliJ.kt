package com.javiersc.hubdle.extensions.kotlin.intellij._internal

import com.javiersc.gradle.properties.extensions.getProperty
import com.javiersc.gradle.properties.extensions.getPropertyOrNull
import com.javiersc.hubdle.HubdleProperty
import com.javiersc.hubdle.HubdleProperty.IntelliJ
import com.javiersc.hubdle.HubdleProperty.JetBrains
import com.javiersc.hubdle.extensions._internal.PluginIds
import com.javiersc.hubdle.extensions._internal.state.HubdleState
import com.javiersc.hubdle.extensions._internal.state.catalogDependency as catalogDep
import com.javiersc.hubdle.extensions._internal.state.hubdleState
import com.javiersc.hubdle.extensions.config.documentation.changelog.GENERATED_CHANGELOG_HTML_DIR_PATH
import com.javiersc.hubdle.extensions.config.documentation.changelog.GENERATED_CHANGELOG_HTML_FILE_PATH
import com.javiersc.hubdle.extensions.config.documentation.changelog._internal.GENERATED_CHANGELOG_HTML_ATTRIBUTE
import com.javiersc.hubdle.extensions.config.explicit.api._internal.configureExplicitApi
import com.javiersc.hubdle.extensions.dependencies._internal.constants.COM_JAVIERSC_GRADLE_GRADLE_EXTENSIONS_MODULE
import com.javiersc.hubdle.extensions.dependencies._internal.constants.COM_JAVIERSC_GRADLE_GRADLE_TEST_EXTENSIONS_MODULE
import com.javiersc.hubdle.extensions.dependencies._internal.constants.COM_JAVIERSC_KOTLIN_KOTLIN_STDLIB_MODULE
import com.javiersc.hubdle.extensions.dependencies._internal.constants.IO_KOTEST_KOTEST_ASSERTIONS_CORE_MODULE
import com.javiersc.hubdle.extensions.dependencies._internal.constants.ORG_JETBRAINS_KOTLINX_KOTLINX_COROUTINES_CORE_MODULE
import com.javiersc.hubdle.extensions.dependencies._internal.constants.ORG_JETBRAINS_KOTLINX_KOTLINX_COROUTINES_TEST_MODULE
import com.javiersc.hubdle.extensions.dependencies._internal.constants.ORG_JETBRAINS_KOTLINX_KOTLINX_SERIALIZATION_CORE_MODULE
import com.javiersc.hubdle.extensions.dependencies._internal.constants.ORG_JETBRAINS_KOTLINX_KOTLINX_SERIALIZATION_JSON_MODULE
import com.javiersc.hubdle.extensions.dependencies._internal.constants.ORG_JETBRAINS_KOTLIN_KOTLIN_TEST_MODULE
import com.javiersc.hubdle.extensions.kotlin._internal.configJvmTarget
import com.javiersc.hubdle.extensions.options.configureDefaultJavaSourceSets
import com.javiersc.hubdle.extensions.options.configureDefaultKotlinSourceSets
import org.gradle.api.Project
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.gradleKotlinDsl
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.jetbrains.intellij.IntelliJPluginExtension
import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.intellij.tasks.PublishPluginTask
import org.jetbrains.intellij.tasks.SignPluginTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler

internal fun configureIntelliJ(project: Project) {
    if (project.hubdleState.kotlin.intellij.isEnabled) {
        project.pluginManager.apply(PluginIds.Kotlin.jvm)
        project.pluginManager.apply(PluginIds.JetBrains.intellij)

        configureIntellijPluginExtension(project)
        configureGeneratedChangelogHtmlDependency(project)
        configurePatchPluginXml(project)

        project.configureExplicitApi()
        project.configJvmTarget()
        project.the<JavaPluginExtension>().configureDefaultJavaSourceSets()
        project.the<KotlinProjectExtension>().configureDefaultKotlinSourceSets()
        project.the<KotlinJvmProjectExtension>().configureJvmDependencies()

        if (project.hubdleState.config.publishing.isEnabled) {
            configurePublishPlugin(project)
            configureSignPlugin(project)
        }
    }
}

private fun configureIntellijPluginExtension(project: Project) {
    val downloadSourcesProperty =
        project.getPropertyOrNull(IntelliJ.downloadSources)?.toBoolean() ?: true

    val updateSinceUntilBuildProperty =
        project.getPropertyOrNull(IntelliJ.updateSinceUntilBuild)?.toBoolean() ?: true

    project.configure<IntelliJPluginExtension> {
        pluginName.set(project.getProperty(HubdleProperty.POM.name))
        downloadSources.set(downloadSourcesProperty)
        type.set(project.getProperty(IntelliJ.type))
        version.set(project.getProperty(IntelliJ.version))
        updateSinceUntilBuild.set(updateSinceUntilBuildProperty)
        project.hubdleState.kotlin.intellij.intellij?.execute(this)
    }
}

private fun configurePatchPluginXml(project: Project) {
    project.tasks.withType<PatchPluginXmlTask>().configureEach { task ->
        task.dependsOn(project.tasks.named("copyGeneratedChangelogHtml"))

        task.version.set("${project.version}")
        task.sinceBuild.set(project.getProperty(IntelliJ.sinceBuild))
        task.untilBuild.set(project.getProperty(IntelliJ.untilBuild))

        val changelogFile = project.layout.buildDirectory.file(GENERATED_CHANGELOG_HTML_FILE_PATH)
        val notes =
            changelogFile.flatMap {
                project.provider {
                    if (it.asFile.exists()) it.asFile.readText() else "No changelog found"
                }
            }
        task.changeNotes.set(notes)

        project.hubdleState.kotlin.intellij.patchPluginXml?.execute(task)
    }
}

private fun configurePublishPlugin(project: Project) {
    project.tasks.withType<PublishPluginTask>().configureEach { task ->
        task.token.set(project.getPropertyOrNull(IntelliJ.publishToken) ?: "")
        project.hubdleState.kotlin.intellij.publishPlugin?.execute(task)
    }
}

private fun configureSignPlugin(project: Project) {
    project.tasks.withType<SignPluginTask>().configureEach { task ->
        val certificate = project.getPropertyOrNull(JetBrains.marketplaceCertificateChain) ?: ""
        val key =
            project.getPropertyOrNull(JetBrains.marketplaceKey)
                ?: project.getPropertyOrNull(HubdleProperty.Signing.gnupgKey) ?: ""

        val passphrase =
            project.getPropertyOrNull(JetBrains.marketplaceKeyPassphrase)
                ?: project.getPropertyOrNull(HubdleProperty.Signing.gnupgPassphrase) ?: ""

        task.certificateChain.set(certificate)
        task.privateKey.set(key)
        task.password.set(passphrase)
        project.hubdleState.kotlin.intellij.signPlugin?.execute(task)
    }
}

private fun configureGeneratedChangelogHtmlDependency(project: Project) {
    val generatedChangelogHtml by
        project.configurations.creating {
            isCanBeConsumed = false
            isCanBeResolved = true
            attributes { attributes ->
                attributes.attribute(
                    LIBRARY_ELEMENTS_ATTRIBUTE,
                    project.objects.named(GENERATED_CHANGELOG_HTML_ATTRIBUTE)
                )
            }
        }

    project.dependencies { generatedChangelogHtml(project(":")) }

    project.tasks.register<Copy>("copyGeneratedChangelogHtml") {
        from(generatedChangelogHtml)
        into(project.layout.buildDirectory.dir(GENERATED_CHANGELOG_HTML_DIR_PATH))
    }
}

internal fun configureKotlinIntellijRawConfig(project: Project) {
    project.hubdleState.kotlin.intellij.rawConfig.kotlin?.execute(project.the())
}

private fun KotlinJvmProjectExtension.configureJvmDependencies() {
    sourceSets.named("main") { set -> set.dependencies { configureMainDependencies() } }
    sourceSets.named("test") { set -> set.dependencies { configureTestDependencies() } }
}

internal val Project.intellijFeatures: HubdleState.Kotlin.IntelliJ.Features
    get() = hubdleState.kotlin.intellij.features

private fun KotlinDependencyHandler.configureMainDependencies() {
    with(project) {
        if (intellijFeatures.coroutines) {
            implementation(catalogDep(ORG_JETBRAINS_KOTLINX_KOTLINX_COROUTINES_CORE_MODULE))
        }
        if (intellijFeatures.extendedGradle) {
            implementation(project.dependencies.gradleApi())
            implementation(project.dependencies.gradleTestKit())
            implementation(project.gradleKotlinDsl())
            implementation(catalogDep(COM_JAVIERSC_GRADLE_GRADLE_EXTENSIONS_MODULE))
        }
        if (intellijFeatures.extendedStdlib) {
            implementation(catalogDep(COM_JAVIERSC_KOTLIN_KOTLIN_STDLIB_MODULE))
        }

        if (intellijFeatures.serialization.isEnabled) {
            project.pluginManager.apply(PluginIds.Kotlin.serialization)
            implementation(catalogDep(ORG_JETBRAINS_KOTLINX_KOTLINX_SERIALIZATION_CORE_MODULE))
            if (intellijFeatures.serialization.useJson) {
                implementation(catalogDep(ORG_JETBRAINS_KOTLINX_KOTLINX_SERIALIZATION_JSON_MODULE))
            }
        }
    }
}

private fun KotlinDependencyHandler.configureTestDependencies() {
    with(project) {
        implementation(catalogDep(ORG_JETBRAINS_KOTLIN_KOTLIN_TEST_MODULE))
        if (intellijFeatures.coroutines) {
            implementation(catalogDep(ORG_JETBRAINS_KOTLINX_KOTLINX_COROUTINES_TEST_MODULE))
        }
        if (intellijFeatures.extendedGradle) {
            implementation(catalogDep(COM_JAVIERSC_GRADLE_GRADLE_TEST_EXTENSIONS_MODULE))
        }
        if (intellijFeatures.extendedTesting) {
            implementation(catalogDep(IO_KOTEST_KOTEST_ASSERTIONS_CORE_MODULE))
        }
    }
}

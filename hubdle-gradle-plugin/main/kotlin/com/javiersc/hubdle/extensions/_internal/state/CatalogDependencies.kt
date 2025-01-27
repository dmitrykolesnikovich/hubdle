package com.javiersc.hubdle.extensions._internal.state

import com.javiersc.gradle.extensions.version.catalogs.catalogNamesWithLibsAtFirst
import com.javiersc.gradle.extensions.version.catalogs.getLibraries
import com.javiersc.gradle.project.extensions.module
import com.javiersc.hubdle.extensions.dependencies._internal.hubdleDependencies
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.gradle.api.internal.artifacts.dependencies.DefaultMinimalDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultMutableVersionConstraint
import org.gradle.kotlin.dsl.findByType

internal val userCatalogsDependencies: UserCatalogsDependencies = UserCatalogsDependencies()

internal val hubdleCatalogsDependencies: HubdleCatalogsDependencies = HubdleCatalogsDependencies()

internal fun Project.catalogDependency(module: String): MinimalExternalModuleDependency =
    checkNotNull(getDependency(module)) {
        "The dependency $module has not been found in any catalog"
    }

private fun Project.getDependency(module: String): MinimalExternalModuleDependency? =
    getCatalogsDependency(module).takeIf { project.module != "${it.module}" }

private fun getCatalogsDependency(module: String): MinimalExternalModuleDependency {
    check(module.split(":").count() == 2) { "The module provided is not correct" }
    return userCatalogsDependencies.dependencies.firstOrNull { "${it.module}" == module }
        ?: hubdleCatalogsDependencies.dependencies.first { "${it.module}" == module }
}

internal data class UserCatalogsDependencies(
    val dependencies: MutableList<MinimalExternalModuleDependency> = mutableListOf(),
) : Configurable {
    override fun configure(project: Project) {
        val catalogExtension: VersionCatalogsExtension? = project.extensions.findByType()
        val libraries: Set<MinimalExternalModuleDependency> =
            catalogExtension
                ?.catalogNamesWithLibsAtFirst
                ?.flatMap(catalogExtension::getLibraries)
                ?.toSet()
                .orEmpty()
        dependencies += libraries
    }
}

internal data class HubdleCatalogsDependencies(
    val dependencies: MutableList<MinimalExternalModuleDependency> = mutableListOf(),
) : Configurable {
    override fun configure(project: Project) {
        dependencies += hubdleDependencies.map(String::buildDefaultMinimalDependency)
    }
}

private fun String.buildDefaultMinimalDependency(): MinimalExternalModuleDependency {
    val (group: String, name: String) = split(":")
    val version: String = split(":").getOrElse(2) { "" }
    return DefaultMinimalDependency(
        DefaultModuleIdentifier.newId(group, name),
        DefaultMutableVersionConstraint.withVersion(version)
    )
}

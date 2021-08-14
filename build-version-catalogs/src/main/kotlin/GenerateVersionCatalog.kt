@file:Suppress("SwallowedException", "TooGenericExceptionCaught", "LongMethod")

import java.io.File

internal fun File.generateVersionCatalog(buildDir: File) {
    this.readLines().apply {
        val catalogStartIndex = indexOfFirst { line -> line.contains("// catalog start") }
        val versionsIndex = indexOfFirst { line -> line.contains("// [versions]") }
        val librariesIndex = indexOfFirst { line -> line.contains("// [libraries]") }
        val catalogEndIndex = indexOfFirst { line -> line.contains("// catalog end") }
        val bundlesIndex =
            indexOfFirst { line -> line.contains("// [bundles]") }.run {
                if (this > 0) this else catalogEndIndex - 1
            }

        val name = extractName(get(catalogStartIndex))

        val versions =
            subList(versionsIndex + 1, librariesIndex - 1).joinToString("\n") { version ->
                version.replace("val ", "")
            }

        val libraries =
            subList(librariesIndex + 1, bundlesIndex - 1).sanitizeLibraries().joinToString("\n") {
                library ->
                val alias = extractAlias(library)
                val module = extractModule(library)
                val version = extractVersion(library)

                if (version != null) {
                    """$alias = { module = "$module", version.ref = "$version" }"""
                } else {
                    """$alias = { module = "$module" }"""
                }
            }

        val bundles: String? =
            try {
                subList(bundlesIndex + 1, catalogEndIndex - 1).sanitizeBundles().joinToString(
                    "\n"
                ) { bundle ->
                    val alias = extractAlias(bundle)
                    val bundles = extractBundles(bundle)
                    """$alias = [$bundles]"""
                }
            } catch (throwable: Throwable) {
                null
            }

        val catalog =
            if (bundles != null) {
                """
                    |[versions]
                    |$versions
                    |
                    |[libraries]
                    |$libraries
                    |
                    |[bundles]
                    |$bundles
                """.trimMargin()
            } else {
                """
                    |[versions]
                    |$versions
                    |
                    |[libraries]
                    |$libraries
                """.trimMargin()
            }

        File(buildDir, "$name.toml").apply {
            parentFile.mkdirs()
            createNewFile()
            writeText(catalog)
        }
    }
}

internal fun extractName(line: String): String {
    return line.dropWhile { char -> char != '-' }.drop(2)
}

internal fun extractAlias(line: String): String {
    return line.replace("val ", "")
        .replaceAfter("=", "")
        .replace(" ", "")
        .replace("=", "")
        .replace("_", "-")
}

internal fun extractModule(line: String): String {
    return if (line.contains("\$")) {
        line.replaceBefore("\"", "").replaceAfterLast(":", "").replace("\"", "").dropLast(1)
    } else {
        line.replaceBefore("\"", "").replace("\"", "")
    }
}

internal fun extractVersion(line: String): String? {
    return if (line.contains("\$")) {
        line.replaceBefore("\$", "").replaceAfter("\"", "").replace("\$", "").replace("\"", "")
    } else {
        null
    }
}

internal fun extractBundles(line: String): String {
    return line
        .replaceBefore("=", "")
        .replace("=", "")
        .replace(" ", "")
        .replace("_", "-")
        .split("+")
        .joinToString(", ") { bundle -> "\"$bundle\"" }
}

internal fun List<String>.sanitizeLibraries(): List<String> {
    return this.asSequence()
        .filter(String::isNotBlank)
        .mapIndexed { index: Int, line: String ->
            if (line.replace(" ", "").startsWith("val")) {
                val nextLine = this.getOrElse(index + 1) { "" }
                if (nextLine.replace(" ", "").startsWith("val") ||
                    nextLine.replace(" ", "").isBlank()
                ) {
                    line
                } else {
                    "$line$nextLine"
                }
            } else ""
        }
        .filter(String::isNotBlank)
        .sorted()
        .toList()
}

internal fun List<String>.sanitizeBundles(): List<String> {
    return this.asSequence()
        .filter(String::isNotBlank)
        .mapIndexed { index: Int, line: String ->
            if (line.replace(" ", "").startsWith("val")) {
                var counter = 1
                var nextLine = this.getOrElse(index + counter) { "" }
                var currentLine = line
                while (nextLine.replace(" ", "").startsWith("val").not() && nextLine.isNotBlank()) {
                    counter++
                    currentLine += nextLine.replace(" ", "")
                    nextLine = this.getOrElse(index + counter) { "" }.replaceBefore("\"", "")
                }
                if (line.replace(" ", "").startsWith("val")) {
                    currentLine
                        .replace(" ", "")
                        .replaceFirst("val", "val ")
                        .replace("=", " = ")
                        .replace("+", " + ")
                        .dropLastWhile(Char::isWhitespace)
                } else ""
            } else ""
        }
        .filter(String::isNotBlank)
        .sorted()
        .toList()
}
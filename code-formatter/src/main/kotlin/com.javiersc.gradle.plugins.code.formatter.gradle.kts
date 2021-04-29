import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    id("com.diffplug.spotless")
}

configure<SpotlessExtension> {
    kotlin {
        target("src/**/*.kt")
        ktfmt().kotlinlangStyle()
    }
}

val ideaDir = file("$rootDir/.idea").also(File::mkdirs)

file("${ideaDir.path}/ktfmt.xml").apply {
    if (!exists()) {
        createNewFile()
        writeText(ktfmtXmlContent)
    }
}

val ktfmtXmlContent: String
    get() =
        """
            <?xml version="1.0" encoding="UTF-8"?>
            <project version="4">
              <component name="KtfmtSettings">
                <option name="enabled" value="true" />
                <option name="uiFormatterStyle" value="Kotlinlang" />
              </component>
            </project>
        """.trimIndent()

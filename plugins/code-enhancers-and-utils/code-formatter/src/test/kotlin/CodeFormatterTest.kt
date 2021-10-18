package com.javiersc.gradle.plugins.code.formatter

import com.javiersc.gradle.plugins.core.test.getResource
import com.javiersc.gradle.plugins.core.test.testSandbox
import io.kotest.matchers.collections.shouldBeSameSizeAs
import io.kotest.matchers.file.shouldBeADirectory
import io.kotest.matchers.file.shouldHaveSameContentAs
import java.io.File
import org.gradle.testkit.runner.BuildResult
import org.junit.Test

class CodeFormatterTest {

    @Test fun `format 1`() = testSandbox(sandboxPath = "sandbox-format-1", test = ::testFormatter)
}

@Suppress("UNUSED_PARAMETER")
fun testFormatter(result: BuildResult, testProjectDir: File) {
    val expect = File("$testProjectDir/library/")
    val actual: File = getResource("sandbox-format-1-actual/library")

    // TODO: replace with `shouldHaveSameStructureAndContentAs` when Kotest 5.0.0 is released
    val expectFiles: List<File> = expect.walkTopDown().toList()
    val actualFiles: List<File> = actual.walkTopDown().toList()

    expectFiles shouldBeSameSizeAs actualFiles

    expectFiles.zip(actualFiles).forEach { (expect, actual) ->
        when {
            expect.isDirectory -> actual.shouldBeADirectory()
            expect.isFile -> expect.shouldHaveSameContentAs(actual)
            else -> error("Unexpected error analyzing file trees")
        }
    }
}
package hm.binkley.dice

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import picocli.CommandLine
import kotlin.reflect.KMutableProperty1

internal class OptionsTest {
    @Test
    fun `should set color with long option`() {
        shouldDefaultThenUpdate(
            Options::color,
            ColorOption.auto,
            ColorOption.none,
            "--color=none"
        )
    }

    @Test
    fun `should set color with short option`() {
        shouldDefaultThenUpdate(
            Options::color,
            ColorOption.auto,
            ColorOption.none,
            "-C", "none"
        )
    }

    @Test
    fun `should set color with fallback`() {
        shouldDefaultThenUpdate(
            Options::color,
            ColorOption.auto,
            ColorOption.always,
            "-C"
        )
    }

    @Test
    fun `should set debug`() {
        shouldDefaultThenUpdate(
            Options::debug,
            false,
            true,
            "--debug",
        )
    }

    @Test
    fun `should set copyright`() {
        shouldDefaultThenUpdate(
            Options::copyright,
            false,
            true,
            "--copyright"
        )
    }

    @Test
    fun `should set demo`() {
        shouldDefaultThenUpdate(
            Options::demo,
            false,
            true,
            "--demo"
        )
    }

    @Test
    fun `should set history`() {
        shouldDefaultThenUpdate(
            Options::history,
            true,
            false,
            "--no-history"
        )
    }

    @Test
    fun `should set minimum with long option`() {
        shouldDefaultThenUpdate(
            Options::minimum,
            Int.MIN_VALUE,
            1,
            "--minimum=1"
        )
    }

    @Test
    fun `should set minimum with short option`() {
        shouldDefaultThenUpdate(
            Options::minimum,
            Int.MIN_VALUE,
            1,
            "-m", "1"
        )
    }

    @Test
    fun `should set prompt with long option`() {
        shouldDefaultThenUpdate(
            Options::prompt,
            DIE_PROMPT,
            "> ",
            "--prompt", "> "
        )
    }

    @Test
    fun `should set prompt with short option`() {
        shouldDefaultThenUpdate(
            Options::prompt,
            DIE_PROMPT,
            "> ",
            "-P", "> "
        )
    }

    @Test
    fun `should set seed with long option`() {
        shouldDefaultThenUpdate(
            Options::seed,
            null,
            1,
            "--seed", "1"
        )
    }

    @Test
    fun `should set test repl`() {
        shouldDefaultThenUpdate(
            Options::testRepl,
            false,
            true,
            "--test-repl"
        )
    }

    @Test
    fun `should set verbose with long option`() {
        shouldDefaultThenUpdate(
            Options::verbose,
            false,
            true,
            "--verbose",
        )
    }

    @Test
    fun `should set verbose with short option`() {
        shouldDefaultThenUpdate(
            Options::verbose,
            false,
            true,
            "-v",
        )
    }

    @Test
    fun `should save command line arguments`() {
        val options = Options()

        CommandLine(options).parseArgs("a", "b")

        options.arguments shouldBe listOf("a", "b")
        options.arguments = listOf("p", "q") // Writeable
    }
}

private fun <T> shouldDefaultThenUpdate(
    prop: KMutableProperty1<Options, T>,
    defaultValue: T,
    updatedValue: T,
    vararg optionFlags: String,
) {
    // Run parseArgs before reading properties so Picocli sets lateinit vars
    // Note also that JaCoCo does not understand branches for lateinit
    val options = Options()

    // First, check default values
    CommandLine(options).parseArgs()
    prop.get(options) shouldBe defaultValue

    // Second, check setting props based on command line flags
    CommandLine(options).parseArgs(*optionFlags)
    prop.get(options) shouldBe updatedValue

    // Third, show the options are mutable from code (not just reflection)
    prop.set(options, prop.get(options))
}

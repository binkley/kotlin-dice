package hm.binkley.dice

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import picocli.CommandLine
import kotlin.reflect.KMutableProperty1

internal class OptionsTest {
    @Test
    fun `should set command spec`() {
        val options = Options()
        CommandLine(options).parseArgs()
        options.commandSpec.shouldNotBeNull()
    }

    @Test
    fun `should set new REPL`() {
        shouldDefaultThenApplyArguments(
            Options::newRepl,
            false,
            true,
            "--new-repl"
        )
    }

    @Test
    fun `should set color with long option`() {
        shouldDefaultThenApplyArguments(
            Options::color,
            ColorOption.auto,
            ColorOption.none,
            "--color=none"
        )
    }

    @Test
    fun `should set color with short option`() {
        shouldDefaultThenApplyArguments(
            Options::color,
            ColorOption.auto,
            ColorOption.none,
            "-C", "none"
        )
    }

    @Test
    fun `should set color with fallback`() {
        shouldDefaultThenApplyArguments(
            Options::color,
            ColorOption.auto,
            ColorOption.always,
            "-C"
        )
    }

    @Test
    fun `should set debug`() {
        shouldDefaultThenApplyArguments(
            Options::debug,
            false,
            true,
            "--debug",
        )
    }

    @Test
    fun `should set copyright`() {
        shouldDefaultThenApplyArguments(
            Options::copyright,
            false,
            true,
            "--copyright"
        )
    }

    @Test
    fun `should set demo`() {
        shouldDefaultThenApplyArguments(
            Options::demo,
            false,
            true,
            "--demo"
        )
    }

    @Test
    fun `should set history`() {
        shouldDefaultThenApplyArguments(
            Options::history,
            true,
            false,
            "--no-history"
        )
    }

    @Test
    fun `should set minimum with long option`() {
        shouldDefaultThenApplyArguments(
            Options::minimum,
            Int.MIN_VALUE,
            1,
            "--minimum=1"
        )
    }

    @Test
    fun `should set minimum with short option`() {
        shouldDefaultThenApplyArguments(
            Options::minimum,
            Int.MIN_VALUE,
            1,
            "-m", "1"
        )
    }

    @Test
    fun `should set prompt with long option`() {
        shouldDefaultThenApplyArguments(
            Options::prompt,
            DIE_PROMPT,
            "> ",
            "--prompt", "> "
        )
    }

    @Test
    fun `should set prompt with short option`() {
        shouldDefaultThenApplyArguments(
            Options::prompt,
            DIE_PROMPT,
            "> ",
            "-P", "> "
        )
    }

    @Test
    fun `should set seed with long option`() {
        shouldDefaultThenApplyArguments(
            Options::seed,
            null,
            1,
            "--seed", "1"
        )
    }

    @Test
    fun `should set test repl`() {
        shouldDefaultThenApplyArguments(
            Options::testRepl,
            false,
            true,
            "--test-repl"
        )
    }

    @Test
    fun `should set verbose with long option`() {
        shouldDefaultThenApplyArguments(
            Options::verbose,
            false,
            true,
            "--verbose",
        )
    }

    @Test
    fun `should set verbose with short option`() {
        shouldDefaultThenApplyArguments(
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

private fun <T> shouldDefaultThenApplyArguments(
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

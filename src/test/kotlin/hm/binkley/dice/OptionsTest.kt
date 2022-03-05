package hm.binkley.dice

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import picocli.CommandLine
import kotlin.reflect.KProperty1

internal class OptionsTest {
    @Test
    fun `should set color with long option`() {
        shouldDefaultThenUpdate(
            Options::color,
            ColorOption.auto,
            ColorOption.always,
            "--color", "always"
        )
    }

    @Test
    fun `should set color with short option`() {
        shouldDefaultThenUpdate(
            Options::color,
            ColorOption.auto,
            ColorOption.always,
            "-C", "always"
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
    fun `should set minimum with long option`() {
        shouldDefaultThenUpdate(
            Options::minimum,
            Int.MIN_VALUE,
            1,
            "--minimum", "1"
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
            COLORFUL_DIE_PROMPT,
            "> ",
            "--prompt", "> "
        )
    }

    @Test
    fun `should set prompt with short option`() {
        shouldDefaultThenUpdate(
            Options::prompt,
            COLORFUL_DIE_PROMPT,
            "> ",
            "-p", "> "
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
}

private fun <T> shouldDefaultThenUpdate(
    prop: KProperty1<Options, T>,
    default: T,
    updated: T,
    vararg flags: String,
) {
    val options = Options()

    prop.get(options) shouldBe default

    CommandLine(options).parseArgs(*flags)

    prop.get(options) shouldBe updated
}

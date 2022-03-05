package hm.binkley.dice

import com.github.stefanbirkner.systemlambda.SystemLambda.restoreSystemProperties
import hm.binkley.dice.Options.Color
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import picocli.CommandLine
import kotlin.reflect.KProperty1

internal class OptionsTest {
    /** @todo How to test and *not* expose innards of the Color enum? */
    @Test
    fun `should alias arguments to color flag`() {
        shouldUpdateColorSysProp(Color.always, "true")
        shouldUpdateColorSysProp(Color.yes, "true")
        shouldUpdateColorSysProp(Color.force, "true")

        shouldUpdateColorSysProp(Color.auto, null)
        shouldUpdateColorSysProp(Color.tty, null)
        shouldUpdateColorSysProp(Color.`if-tty`, null)

        shouldUpdateColorSysProp(Color.never, "false")
        shouldUpdateColorSysProp(Color.no, "false")
        shouldUpdateColorSysProp(Color.none, "false")
    }

    @Test
    fun `should set debug`() {
        shouldChangeFromDefault(
            Options::debug,
            false,
            true,
            "--debug",
        )
    }

    @Test
    fun `should set color`() {
        shouldChangeFromDefault(
            Options::color,
            Color.auto,
            Color.always,
            "--color", "always"
        )
    }

    @Test
    fun `should set copyright`() {
        shouldChangeFromDefault(
            Options::copyright,
            false,
            true,
            "--copyright"
        )
    }

    @Test
    fun `should set demo`() {
        shouldChangeFromDefault(
            Options::demo,
            false,
            true,
            "--demo"
        )
    }

    @Test
    fun `should set minimum`() {
        shouldChangeFromDefault(
            Options::minimum,
            Int.MIN_VALUE,
            1,
            "--minimum", "1"
        )
    }

    @Test
    fun `should set prompt`() {
        shouldChangeFromDefault(
            Options::prompt,
            COLORFUL_DIE_PROMPT,
            "> ",
            "--prompt",  "> "
        )
    }

    @Test
    fun `should set seed`() {
        shouldChangeFromDefault(
            Options::seed,
            null,
            1,
            "--seed", "1"
        )
    }

    @Test
    fun `should set verbose`() {
        shouldChangeFromDefault(
            Options::verbose,
            false,
            true,
            "--verbose",
        )
    }
}

private fun shouldUpdateColorSysProp(
    color: Color,
    sysPropValue: String?,
) {
    restoreSystemProperties {
        color.install()
        System.getProperty("picocli.ansi") shouldBe sysPropValue
    }
}

private fun <T> shouldChangeFromDefault(
    prop: KProperty1<Options, T>,
    default: T,
    changed: T,
    vararg flags: String,
) {
    val options = Options()

    prop.get(options) shouldBe default

    CommandLine(options).parseArgs(*flags)

    prop.get(options) shouldBe changed
}

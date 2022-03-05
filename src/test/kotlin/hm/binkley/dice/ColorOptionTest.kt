package hm.binkley.dice

import com.github.stefanbirkner.systemlambda.SystemLambda.restoreSystemProperties
import hm.binkley.dice.ColorOption.always
import hm.binkley.dice.ColorOption.auto
import hm.binkley.dice.ColorOption.force
import hm.binkley.dice.ColorOption.`if-tty`
import hm.binkley.dice.ColorOption.never
import hm.binkley.dice.ColorOption.no
import hm.binkley.dice.ColorOption.none
import hm.binkley.dice.ColorOption.tty
import hm.binkley.dice.ColorOption.yes
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ColorOptionTest {
    @Test
    fun `should alias arguments to color option`() {
        shouldUpdateColorSysProp(always, "true")
        shouldUpdateColorSysProp(yes, "true")
        shouldUpdateColorSysProp(force, "true")

        shouldUpdateColorSysProp(auto, null)
        shouldUpdateColorSysProp(tty, null)
        shouldUpdateColorSysProp(`if-tty`, null)

        shouldUpdateColorSysProp(never, "false")
        shouldUpdateColorSysProp(no, "false")
        shouldUpdateColorSysProp(none, "false")
    }
}

private fun shouldUpdateColorSysProp(
    color: ColorOption,
    sysPropValue: String?,
) {
    restoreSystemProperties {
        color.install()
        System.getProperty("picocli.ansi") shouldBe sysPropValue
    }
}

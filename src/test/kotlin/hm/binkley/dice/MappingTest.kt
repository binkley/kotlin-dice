package hm.binkley.dice

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test

internal class MappingTest {
    @Test
    fun `should map roll`() {
        expect(3.mapTo {
            when (it) {
                3 -> "Good!"
                else -> null
            }
        }).toEqual("Good!")
    }

    @Test
    fun `should complain for rolls not in mapping`() {
        expect {
            2.mapTo {
                when (it) {
                    3 -> "Good!"
                    else -> null
                }
            }
        }.toThrow<IllegalStateException>()
    }
}

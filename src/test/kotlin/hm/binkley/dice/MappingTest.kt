package hm.binkley.dice

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class MappingTest {
    @Test
    fun `should map roll`() {
        3.mapTo {
            when (it) {
                3 -> "Good!"
                else -> null
            }
        } shouldBe "Good!"
    }

    @Test
    fun `should complain for rolls not in mapping`() {
        shouldThrow<IllegalStateException> {
            2.mapTo {
                when (it) {
                    3 -> "Good!"
                    else -> null
                }
            }
        }
    }
}

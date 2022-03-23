package hm.binkley.dice.rolling

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.parboiled.buffers.DefaultInputBuffer
import org.parboiled.errors.ActionError
import org.parboiled.errors.ActionException
import org.parboiled.matchers.ActionMatcher
import org.parboiled.support.MatcherPath
import org.parboiled.support.MatcherPath.Element

internal class DiceExceptionTest {
    @Test
    fun `should use default formatter for action errors`() {
        val input = "abcdef"
        val e = BadExpressionException(
            listOf(
                ActionError(
                    DefaultInputBuffer(input.toCharArray()),
                    2, // 0-based
                    "Mary had a little lamb",
                    MatcherPath(
                        Element(ActionMatcher { false }, 2, 0),
                        null
                    ),
                    ActionException("The lamb was sure to go"),
                )
            )
        )

        e.message!!.trim() shouldBe """
Mary had a little lamb (line 1, pos 3):
abcdef
  ^            
        """.trim()
    }
}

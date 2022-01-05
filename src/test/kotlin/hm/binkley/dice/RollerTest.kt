package hm.binkley.dice

import hm.binkley.dice.DieShift.ONE
import hm.binkley.dice.DieShift.ZERO
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class RollerTest {
    @Test
    fun `should report roll details`() {
        val random = stableSeedForEachTest()
        val assertOnRoll = OnRoll {
            it.d shouldBe 6
            it.n shouldBe 7
            it.reroll shouldBe 2
            it.keep shouldBe 3
            it.explode shouldBe 4
        }

        Roller(
            d = 6,
            dieShift = ONE,
            n = 7,
            reroll = 2,
            keep = 3,
            explode = 4,
            random = random,
            reporting = assertOnRoll
        ).rollDice()
    }

    @MethodSource("args")
    @ParameterizedTest
    fun `should roll`(
        n: Int,
        dieShift: DieShift,
        d: Int,
        reroll: Int,
        keep: Int,
        explode: Int,
        expected: Int,
    ) {
        val random = stableSeedForEachTest()

        val result = Roller(d, dieShift, n, reroll, keep, explode, random)
            .rollDice()

        result shouldBe expected
    }

    internal companion object {
        @JvmStatic
        @Suppress("unused")
        fun args(): Stream<Arguments> = Stream.of(
            Arguments.of(3, ONE, 6, 0, 3, 7, 10),
            Arguments.of(1, ZERO, 6, 0, 0, 0, 0),
            Arguments.of(10, ONE, 3, 0, 10, 3, 20),
            Arguments.of(10, ONE, 3, 0, 10, 2, 49),
            Arguments.of(4, ONE, 6, 0, 3, 7, 10),
            Arguments.of(4, ONE, 6, 0, -3, 7, 6),
            Arguments.of(6, ONE, 4, 0, -5, 4, 20),
            Arguments.of(3, ONE, 3, 1, 2, 3, 10),
            Arguments.of(1, ONE, 6, 0, 1, 7, 4)
        )
    }
}

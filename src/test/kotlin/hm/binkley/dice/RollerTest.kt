package hm.binkley.dice

import ch.tutteli.atrium.api.fluent.en_GB.toBe
import ch.tutteli.atrium.api.verbs.expect
import java.util.stream.Stream
import kotlin.random.Random
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

private fun stableSeedForEachTest() = Random(1L)

@TestInstance(PER_CLASS)
internal class RollerTest {
    @MethodSource("args")
    @ParameterizedTest
    fun `should roll`(
        n: Int,
        d: Int,
        reroll: Int,
        keep: Int,
        explode: Int,
        expected: Int
    ) {
        val random = stableSeedForEachTest()

        val result = Roller(n, d, reroll, keep, explode, random).rollDice()

        expect(result).toBe(expected)
    }

    internal companion object {
        @JvmStatic
        @Suppress("unused")
        fun args(): Stream<Arguments> = Stream.of(
            Arguments.of(3, 6, 0, 3, 7, 10),
            Arguments.of(10, 3, 0, 10, 3, 20),
            Arguments.of(10, 3, 0, 10, 2, 49),
            Arguments.of(4, 6, 0, 3, 7, 10),
            Arguments.of(4, 6, 0, -3, 7, 6),
            Arguments.of(6, 4, 0, -5, 4, 20),
            Arguments.of(3, 3, 1, 2, 3, 10),
            Arguments.of(1, 6, 0, 1, 7, 4)
        )
    }
}

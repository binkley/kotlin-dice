package hm.binkley.dice

import hm.binkley.dice.DieBase.ONE
import hm.binkley.dice.DieBase.ZERO
import kotlin.random.Random

private typealias ReportType = (DiceExpression, Int) -> RollAction

/**
 * Represents rolling dice of a given `d` number of sides, eg, d12, and
 * summing the results.
 *
 * *NB* &mdash; Exploding dice can result in more than `d` dice in total.
 *
 * @see [DiceParser] for turning dice expression strings into `Roller`
 * instances
 */
data class Roller(
    val expression: DiceExpression,
    /** The RNG.  Tests use `stableSeedForEachTest()` for reproducibility. */
    private val random: Random,
    /** Reports on individual roll outcomes for feedback. */
    private val reporting: RollReporter,
) {
    fun rollDice(): Int {
        val rolls = generateSequence {
            rollPlain()
        }.take(expression.n).toList().sorted()

        val kept =
            if (expression.keep < 0) rolls.keepLowest()
            else rolls.keepHighest()

        return (kept.sum() + kept.rollExplosions().sum()) *
                expression.multiply
    }

    private fun List<Int>.keepLowest(): List<Int> {
        subList(-expression.keep, expression.n).forEach {
            report(DroppedRoll(expression, it))
        }
        return subList(0, -expression.keep)
    }

    private fun List<Int>.keepHighest(): List<Int> {
        subList(0, expression.n - expression.keep).forEach {
            report(DroppedRoll(expression, it))
        }
        return subList(expression.n - expression.keep, expression.n)
    }

    private fun List<Int>.rollExplosions(): List<Int> {
        val explosions = mutableListOf<Int>()
        forEach {
            var roll = it
            while (roll >= expression.explode) {
                roll = rollExplosion()
                explosions += roll
            }
        }
        return explosions
    }

    private fun rollPlain() = rollAndTrack(::PlainRoll, ::PlainReroll)

    private fun rollExplosion() = rollAndTrack(
        ::ExplodedRoll,
        ::ExplodedReroll
    )

    private fun rollAndTrack(
        onRoll: ReportType, onReroll: ReportType,
    ): Int {
        var roll = rollDie()
        report(onRoll(expression, roll))
        while (roll <= expression.reroll) {
            roll = rollDie()
            report(onReroll(expression, roll))
        }
        return roll
    }

    private fun rollDie(): Int {
        val roll = random.nextInt(0, expression.d)
        return when (expression.dieBase) {
            ZERO -> roll
            ONE -> roll + 1
        }
    }

    private fun report(action: RollAction) = reporting.onRoll(action)
}

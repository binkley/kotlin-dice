package hm.binkley.dice.rolling

import hm.binkley.dice.rolling.KeepCount.Companion.keep
import kotlin.random.Random

private typealias ReportType = (DiceExpression, Int) -> RolledDice

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
    private val random: Random,
    private val reporting: RollReporter,
    private val parsed: DiceExpression,
) {
    fun rollDice() = with(parsed) {
        if (dieBase >= explodeHigh)
            throw ExplodingForeverException(expression, explodeHigh)

        val rolls = generateSequence {
            rollPlain()
        }.take(diceCount).toList()

        val kept = rolls.sorted().keepOrDrop()

        (kept.sum() + kept.rollExplosions().sum()) * multiply
    }

    private fun List<Int>.keepOrDrop(): List<Int> {
        val (kept, dropped) = keep(parsed.keepCount)
        dropped.forEach { report(DroppedRoll(parsed, it)) }
        return kept
    }

    private fun List<Int>.rollExplosions(): List<Int> {
        val explosions = mutableListOf<Int>()
        forEach {
            var roll = it
            while (roll >= parsed.explodeHigh) {
                roll = rollExplosion()
                explosions += roll
            }
        }
        return explosions
    }

    private fun rollPlain() = rollAndReport(
        ::PlainRoll,
        ::PlainReroll
    )

    private fun rollExplosion() = rollAndReport(
        ::ExplodedRoll,
        ::ExplodedReroll
    )

    private fun rollAndReport(
        onRoll: ReportType,
        onReroll: ReportType,
    ) = with(parsed) {
        var roll = rollDie()
        report(onRoll(this, roll))
        while (roll <= rerollLow) {
            roll = rollDie()
            report(onReroll(this, roll))
        }
        roll
    }

    private fun rollDie() = with(parsed) {
        dieBase + random.nextInt(0, dieSides)
    }

    private fun report(dice: RolledDice) = reporting.onRoll(dice)
}

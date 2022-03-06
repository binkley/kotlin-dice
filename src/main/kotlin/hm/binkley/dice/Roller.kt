package hm.binkley.dice

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
    private val expression: String,
    private val random: Random,
    private val reporting: RollReporter,
    private val parsed: DiceExpression,
) {
    fun rollDice() = with(parsed) {
        with(parsed) {
            if (dieBase >= explodeHigh)
                throw ExplodingForeverException(expression, explodeHigh)
        }

        val rolls = generateSequence {
            rollPlain()
        }.take(diceCount).toList().sorted()

        val kept =
            if (keepCount < 0) rolls.keepLowest()
            else rolls.keepHighest()

        (kept.sum() + kept.rollExplosions().sum()) * multiply
    }

    private fun List<Int>.keepLowest() = with(parsed) {
        subList(-keepCount, diceCount).forEach {
            report(DroppedRoll(this, it))
        }
        subList(0, -keepCount)
    }

    private fun List<Int>.keepHighest() = with(parsed) {
        subList(0, diceCount - keepCount).forEach {
            report(DroppedRoll(this, it))
        }
        subList(diceCount - keepCount, diceCount)
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

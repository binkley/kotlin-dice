package hm.binkley.dice

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
    fun rollDice() = with(expression) {
        val rolls = generateSequence {
            rollPlain()
        }.take(diceCount).toList().sorted()

        val kept =
            if (keepCount < 0) rolls.keepLowest()
            else rolls.keepHighest()

        (kept.sum() + kept.rollExplosions().sum()) * multiply
    }

    private fun List<Int>.keepLowest() = with(expression) {
        subList(-keepCount, diceCount).forEach {
            report(DroppedRoll(this, it))
        }
        subList(0, -keepCount)
    }

    private fun List<Int>.keepHighest() = with(expression) {
        subList(0, diceCount - keepCount).forEach {
            report(DroppedRoll(this, it))
        }
        subList(diceCount - keepCount, diceCount)
    }

    private fun List<Int>.rollExplosions(): List<Int> {
        val explosions = mutableListOf<Int>()
        forEach {
            var roll = it
            while (roll >= expression.explodeHigh) {
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
    ) = with(expression) {
        var roll = rollDie()
        report(onRoll(this, roll))
        while (roll <= rerollLow) {
            roll = rollDie()
            report(onReroll(this, roll))
        }
        roll
    }

    private fun rollDie() = with(expression) {
        random.nextInt(0, dieSides) + dieBase.value
    }

    private fun report(action: RollAction) = reporting.onRoll(action)
}

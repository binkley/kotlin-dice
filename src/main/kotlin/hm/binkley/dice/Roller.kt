package hm.binkley.dice

import hm.binkley.dice.DieShift.ONE
import hm.binkley.dice.DieShift.ZERO
import kotlin.random.Random

private typealias ReportType = (Roller, Int) -> RollAction

val DoNothing = OnRoll { }

/**
 * Represents rolling dice of a given [d] number of sides, eg, d12, and
 * summing the results.
 *
 * *NB* &mdash; Exploding dice can result in more than [n] dice in total.
 */
data class Roller(
    /** The number of die sides, eg, d12 (the 12). */
    val d: Int,
    /**
     * Whether the die is one-based (standard, eg, 1-6) or zero-based (eg,
     * 0-5).
     */
    val dieShift: DieShift,
    /** The number of dice to roll, eg, 3d6 (the three). */
    val n: Int,
    /**
     * Reroll die values this value or lower.  This is not the same as
     * "dropping" dice, as rolling continues to meet [n] (or possibly more
     * dice with [explode]).
     *
     * @todo Syntax and support for comparisons other than less-than-or-equal
     */
    val reroll: Int,
    /**
     * Keep rolls:
     * * If positive, keep the highest values (ie, top N die rolls)
     * * If negative, keep the lowest values (ie, bottom N die rolls)
     * * If zero, keep all values regardless of die roll
     */
    val keep: Int,
    /** Continue rolling more dice while the roll is this value or greater. */
    val explode: Int,
    /** The RNG.  Tests will prefer a fixed seed for reproducibility. */
    private val random: Random,
    /** Reports back on roll outcomes, potentially for logging or feedback. */
    private val reporting: OnRoll = DoNothing,
) {
    fun rollDice(): Int {
        val rolls = generateSequence {
            rollPlain()
        }.take(n).toList().sorted()

        val kept =
            if (keep < 0) rolls.keepLowest()
            else rolls.keepHighest()

        return kept.sum() + kept.rollExplosions().sum()
    }

    private fun List<Int>.keepLowest(): List<Int> {
        subList(-keep, n).forEach {
            report(DroppedRoll(this@Roller, it))
        }
        return subList(0, -keep)
    }

    private fun List<Int>.keepHighest(): List<Int> {
        subList(0, n - keep).forEach {
            report(DroppedRoll(this@Roller, it))
        }
        return subList(n - keep, n)
    }

    private fun List<Int>.rollExplosions(): List<Int> {
        val explosions = mutableListOf<Int>()
        forEach {
            var roll = it
            while (roll >= explode) {
                roll = rollExplosion()
                explosions += roll
            }
        }
        return explosions
    }

    private fun rollPlain() =
        rollDieAndTrack(::PlainRoll, ::PlainReroll)

    private fun rollExplosion() =
        rollDieAndTrack(::ExplodedRoll, ::ExplodedReroll)

    private fun rollDieAndTrack(
        onRoll: ReportType, onReroll: ReportType,
    ): Int {
        var roll = rollDie()
        report(onRoll(this, roll))
        while (roll <= reroll) {
            roll = rollDie()
            report(onReroll(this, roll))
        }
        return roll
    }

    private fun rollDie(): Int {
        val roll = random.nextInt(0, d)
        return when (dieShift) {
            ZERO -> roll
            ONE -> roll + 1
        }
    }

    private fun report(action: RollAction) = reporting.onRoll(action)
}

package hm.binkley.dice

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
    /** The number of die sides, eg, d12. */
    val d: Int,
    /** The number of dice to roll. */
    val n: Int,
    /**
     * Reroll die values this value or lower.  This is not the same as
     * "dropping" dice, as rolling continues to meet [n] (or possibly more
     * with [explode]).
     *
     * @todo Syntax and support for other comparisons than less-than-or-equal
     */
    val reroll: Int,
    /**
     * Keep rolls:
     * * If positive, keep highest value (ie, top N)
     * * If negative, keep lowest value (ie, bottom N)
     * * If zero, keep all
     */
    val keep: Int,
    /** Continue rolling more dice while the roll is this value or greater. */
    val explode: Int,
    /** The RNG.  Tests will prefer a fixed seed for reproducibility. */
    private val random: Random,
    /** Reports back on roll outcomes, potentially for logging or feedback. */
    private val callback: OnRoll = DoNothing,
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

    private fun rollDie() = random.nextInt(0, d) + 1

    private fun report(action: RollAction) = callback.onRoll(action)
}

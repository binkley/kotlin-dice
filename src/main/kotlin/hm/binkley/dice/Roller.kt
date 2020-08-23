package hm.binkley.dice

import kotlin.random.Random

private typealias ReportType = (Roller, Int) -> RollAction

val DoNothing = OnRoll { }

data class Roller(
    val n: Int,
    val d: Int,
    val reroll: Int,
    val keep: Int,
    val explode: Int,
    private val random: Random,
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
        rollReportedDie(::PlainRoll, ::PlainReroll)

    private fun rollExplosion() =
        rollReportedDie(::ExplodedRoll, ::ExplodedReroll)

    private fun rollReportedDie(
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

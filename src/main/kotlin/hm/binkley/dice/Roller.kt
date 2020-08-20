package hm.binkley.dice

import hm.binkley.dice.RollType.EXPLODED
import hm.binkley.dice.RollType.PLAIN
import kotlin.random.Random

private enum class RollType {
    PLAIN, EXPLODED
}

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
        val rolls = (1..n).map {
            rollSpecialDie(PLAIN)
        }.sorted()

        val kept: List<Int> =
            if (keep < 0) keepLowest(rolls)
            else keepHighest(rolls)

        return kept.sum() + rollExplosions(kept).sum()
    }

    private fun keepLowest(rolls: List<Int>): List<Int> {
        rolls.subList(-keep, n).forEach {
            report(DroppedRoll(this, it))
        }
        return rolls.subList(0, -keep)
    }

    private fun keepHighest(rolls: List<Int>): List<Int> {
        rolls.subList(0, n - keep).forEach {
            report(DroppedRoll(this, it))
        }
        return rolls.subList(n - keep, n)
    }

    private fun rollExplosions(keep: List<Int>): List<Int> {
        val explosions = mutableListOf<Int>()
        keep.forEach {
            var roll = it
            while (roll >= explode) {
                roll = rollExplosion()
                explosions += roll
            }
        }
        return explosions
    }

    private fun rollExplosion() = rollSpecialDie(EXPLODED)

    private fun rollSpecialDie(type: RollType): Int {
        var roll = rollDie()
        report(when (type) {
            PLAIN -> PlainRoll(this, roll)
            EXPLODED -> ExplodedRoll(this, roll)
        })
        while (roll <= reroll) {
            roll = rollDie()
            report(when (type) {
                PLAIN -> PlainReroll(this, roll)
                EXPLODED -> ExplodedReroll(this, roll)
            })
        }
        return roll
    }

    private fun rollDie() = random.nextInt(0, d) + 1

    private fun report(action: RollAction) = callback.onRoll(action)
}

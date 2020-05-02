package hm.binkley.dice

import kotlin.random.Random

interface OnRoll {
    fun onRoll(message: String)
}

object DoNothing : OnRoll {
    override fun onRoll(message: String) = Unit
}

data class Roller(
    private val n: Int,
    private val d: Int,
    private val reroll: Int,
    private val keep: Int,
    private val explode: Int,
    private val random: Random,
    private val callback: OnRoll = DoNothing
) {
    fun rollDice(): Int {
        val rolls = (1..n).map {
            rollSpecialDie("")
        }.sorted()

        val kept: List<Int> =
            if (keep < 0) keepLowest(rolls)
            else keepHighest(rolls)

        return kept.sum() + rollExplosions(kept).sum()
    }

    private fun keepLowest(rolls: List<Int>): List<Int> {
        rolls.subList(-keep, n).forEach {
            callback.onRoll("drop -> $it")
        }
        return rolls.subList(0, -keep)
    }

    private fun keepHighest(rolls: List<Int>): List<Int> {
        rolls.subList(0, n - keep).forEach {
            callback.onRoll("drop -> $it")
        }
        return rolls.subList(n - keep, n)
    }

    private fun rollSpecialDie(prefix: String): Int {
        var roll = rollDie()
        callback.onRoll("${prefix}roll(d$d) -> $roll")
        while (roll <= reroll) {
            roll = rollDie()
            callback.onRoll("${prefix}reroll(d$d) -> $roll")
        }
        return roll
    }

    private fun rollExplosions(keep: List<Int>): List<Int> {
        val explosions = ArrayList<Int>()
        keep.forEach {
            var roll = it
            while (roll >= explode) {
                roll = rollExplosion()
                explosions += roll
            }
        }
        return explosions
    }

    private fun rollExplosion() = rollSpecialDie("!")

    private fun rollDie() = random.nextInt(0, d) + 1
}

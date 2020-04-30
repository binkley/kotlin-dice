package hm.binkley.dice

import kotlin.random.Random

data class Roller(
    private var n: Int,
    private var d: Int,
    private var reroll: Int,
    private var keep: Int,
    private var explode: Int,
    private val random: Random
) {
    fun rollDice(): Int {
        val rolls = (1..n).map {
            rollSpecialDie("")
        }.toMutableList()

        rolls.sort()

        val kept: List<Int> =
            if (keep < 0) keepLowest(rolls)
            else keepHighest(rolls)

        return kept.sum() + rollExplosions(kept)
    }

    private fun keepLowest(rolls: List<Int>): List<Int> {
        if (verbose) rolls.subList(-keep, n).forEach {
            println("drop -> $it")
        }
        return rolls.subList(0, -keep)
    }

    private fun keepHighest(rolls: List<Int>): List<Int> {
        if (verbose) rolls.subList(0, n - keep).forEach {
            println("drop -> $it")
        }
        return rolls.subList(n - keep, n)
    }

    private fun rollSpecialDie(prefix: String): Int {
        var roll = rollDie()
        if (verbose) println("${prefix}roll(d$d) -> $roll")
        while (roll <= reroll) {
            roll = rollDie()
            if (verbose) println("${prefix}reroll(d$d) -> $roll")
        }
        return roll
    }

    private fun rollExplosions(keep: List<Int>): Int {
        var total = 0
        keep.forEach {
            var roll = it
            while (roll >= explode) {
                roll = rollExplosion()
                total += roll
            }
        }
        return total
    }

    private fun rollExplosion() = rollSpecialDie("!")

    private fun rollDie() = random.nextInt(0, d) + 1
}

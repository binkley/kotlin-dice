@file:Suppress("MemberVisibilityCanBePrivate")

package hm.binkley.dice

import hm.binkley.dice.DiceParser.Companion.roll
import org.parboiled.BaseParser
import org.parboiled.Parboiled.createParser
import org.parboiled.Rule
import org.parboiled.annotations.BuildParseTree
import org.parboiled.errors.ErrorUtils.printParseError
import org.parboiled.parserunners.ReportingParseRunner
import org.parboiled.support.ParsingResult
import java.lang.System.err
import kotlin.random.Random

internal var verbose = false

/**
 * A dice expression evaluator.
 *
 * See [kotlin dice](https://github.com/binkley/kotlin-dice)
 *
 * @see [roll]
 */
@BuildParseTree
open class DiceParser(
    private val random: Random = Random.Default
) : BaseParser<Int>() {
    private var n: Int? = null
    private var d: Int? = null
    private var reroll: Int? = null
    private var keep: Int? = null
    private var explode: Int? = null
    private var adjustment: Int? = null

    open fun diceExpression(): Rule = Sequence(
        rollExpression(),
        maybeRollMore(),
        maybeAdjust()
    )

    internal open fun rollExpression() = Sequence(
        rollCount(),
        dieType(),
        maybeRerollLow(),
        maybeKeepFewer(),
        maybeExplode(),
        rollTheDice()
    )

    internal open fun rollCount() = Sequence(
        Optional(number()),
        recordRollCount()
    )

    internal fun recordRollCount(): Boolean {
        n = matchRollCount()
        return true
    }

    internal fun matchRollCount() = matchOrDefault("1").toInt()

    internal open fun number() = Sequence(
        OneOrMore(CharRange('1', '9')),
        ZeroOrMore(CharRange('0', '9'))
    )

    internal open fun dieType() = Sequence(
        Ch('d'),
        FirstOf(
            number(),
            Ch('%')
        ),
        recordDieType()
    )

    internal fun recordDieType(): Boolean {
        d = matchDieType()
        return true
    }

    internal fun matchDieType() = when (val match = match()) {
        "%" -> 100
        else -> match.toInt()
    }

    internal open fun maybeRerollLow() = Sequence(
        Optional(
            Ch('r'),
            number()
        ),
        recordRerollLow()
    )

    internal fun recordRerollLow(): Boolean {
        reroll = matchRerollLow()
        return true
    }

    internal fun matchRerollLow() = when (val match = match()) {
        "" -> 0
        else -> match.substring(1).toInt()
    }

    internal open fun maybeKeepFewer() = Sequence(
        Optional(
            FirstOf(
                Ch('h'),
                Ch('l')
            ),
            number()
        ),
        recordKeepFewer()
    )

    internal fun recordKeepFewer(): Boolean {
        keep = matchKeepFewer()
        return true
    }

    internal fun matchKeepFewer(): Int {
        val match = match()
        return when {
            match.startsWith('h') -> match.substring(1).toInt()
            match.startsWith('l') -> -match.substring(1).toInt()
            else -> n!!
        }
    }

    internal open fun maybeExplode() = Sequence(
        Optional(
            Ch('!'),
            Optional(number())
        ),
        recordExplode()
    )

    internal fun recordExplode(): Boolean {
        explode = matchExplode()
        return true
    }

    internal fun matchExplode() = when (val match = match()) {
        "" -> d!! + 1
        "!" -> d!!
        else -> match.substring(1).toInt()
    }

    internal fun rollTheDice(): Boolean {
        return push(
            rollDice(
                n!!,
                d!!,
                reroll!!,
                keep!!,
                explode!!,
                random
            )
        )
    }

    internal open fun maybeRollMore() = ZeroOrMore(
        Sequence(
            rememberAddOrSubtract(),
            rollExpression(),
            applyAddOrSubtract(),
            updateRunningTotal()
        )
    )

    internal open fun rememberAddOrSubtract() = Sequence(
        FirstOf(
            Ch('+'),
            Ch('-')
        ),
        push(matchAddOrSubtract())
    )

    internal fun matchAddOrSubtract() = if ("+" == match()) 1 else -1

    internal fun applyAddOrSubtract() = push(pop() * pop())

    internal fun updateRunningTotal() = push(pop() + pop())

    internal open fun maybeAdjust() = Optional(
        Sequence(
            rememberAddOrSubtract(),
            number(),
            pushAdjustment(),
            applyAddOrSubtract(),
            updateRunningTotal()
        )
    )

    internal fun pushAdjustment(): Boolean {
        val adjustment = matchAdjustment()
        this.adjustment = adjustment
        return push(adjustment)
    }

    internal fun matchAdjustment() = match().toInt()

    companion object {
        /**
         * Creates a dice expression evaluator using the default random
         * number generator.
         *
         * Note: an _expensive_ call: it recreates the parser for each call.
         */
        fun roll(expression: String): ParsingResult<Int> =
            ReportingParseRunner<Int>(
                createParser(DiceParser::class.java).diceExpression()
            ).run(expression)
    }
}

private fun rollDice(
    n: Int,
    d: Int,
    reroll: Int,
    keep: Int,
    explode: Int,
    random: Random
): Int {
    val rolls = (1..n).map {
        rollSpecialDie(
            "",
            d,
            reroll,
            random
        )
    }.toMutableList()

    rolls.sort()

    val kept: List<Int> =
        if (keep < 0) keepLowest(
            rolls,
            n,
            keep
        )
        else keepHighest(rolls, n, keep)

    return kept.sum() + rollExplosions(
        kept,
        d,
        reroll,
        explode,
        random
    )
}

private fun keepLowest(rolls: List<Int>, n: Int, keep: Int): List<Int> {
    if (verbose) rolls.subList(-keep, n).forEach {
        println("drop -> $it")
    }
    return rolls.subList(0, -keep)
}

private fun keepHighest(rolls: List<Int>, n: Int, keep: Int): List<Int> {
    if (verbose) rolls.subList(0, n - keep).forEach {
        println("drop -> $it")
    }
    return rolls.subList(n - keep, n)
}

private fun rollSpecialDie(
    prefix: String,
    d: Int,
    reroll: Int,
    random: Random
): Int {
    var roll = rollDie(d, random)
    if (verbose) println("${prefix}roll(d$d) -> $roll")
    while (roll <= reroll) {
        roll = rollDie(d, random)
        if (verbose) println("${prefix}reroll(d$d) -> $roll")
    }
    return roll
}

private fun rollExplosions(
    keep: List<Int>,
    d: Int,
    reroll: Int,
    explode: Int,
    random: Random
): Int {
    var total = 0
    keep.forEach {
        var roll = it
        while (roll >= explode) {
            roll = rollExplosion(
                d,
                reroll,
                random
            )
            total += roll
        }
    }
    return total
}

private fun rollExplosion(d: Int, reroll: Int, random: Random) =
    rollSpecialDie("!", d, reroll, random)

private fun rollDie(d: Int, random: Random) =
    random.nextInt(0, d) + 1

fun main() {
    verbose = true

    showRolls("3d6")
    showRolls("3d6+1")
    showRolls("3d6-1")
    showRolls("10d3!")
    showRolls("10d3!2")
    showRolls("4d6h3")
    showRolls("4d6l3")
    showRolls("3d6+2d4")
    showRolls("d%")
    showRolls("6d4l5!")
    showRolls("3d3r1h2!")
    showRolls("blah")
    showRolls("d6")
}

private fun showRolls(expression: String) {
    println("---")
    println("Rolling $expression")
    val result = roll(expression)
    result.parseErrors.forEach {
        err.println(printParseError(it))
    }
    if (!result.hasErrors())
        println("RESULT -> ${result.resultValue}")
}

@file:Suppress("MemberVisibilityCanBePrivate")

package hm.binkley.dice

import hm.binkley.dice.DiceParser.Companion.roll
import org.parboiled.BaseParser
import org.parboiled.Parboiled.createParser
import org.parboiled.Rule
import org.parboiled.annotations.BuildParseTree
import org.parboiled.parserunners.ReportingParseRunner
import org.parboiled.support.ParsingResult
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
    // These properties define the current roll expression
    private var n: Int? = null
    private var d: Int? = null
    private var reroll: Int? = null
    private var keep: Int? = null
    private var explode: Int? = null

    /** The main entry point for the parser. */
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
        n = matchOrDefault("1").toInt()
        return true
    }

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
        d = when (val match = match()) {
            "%" -> 100
            else -> match.toInt()
        }
        return true
    }

    internal open fun maybeRerollLow() = Sequence(
        Optional(
            Ch('r'),
            number()
        ),
        recordRerollLow()
    )

    internal fun recordRerollLow(): Boolean {
        reroll = when (val match = match()) {
            "" -> 0
            else -> match.substring(1).toInt()
        }
        return true
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
        val match = match()
        keep = when {
            match.startsWith('h') -> match.substring(1).toInt()
            match.startsWith('l') -> -match.substring(1).toInt()
            else -> n!!
        }
        return true
    }

    internal open fun maybeExplode() = Sequence(
        Optional(
            Ch('!'),
            Optional(number())
        ),
        recordExplode()
    )

    internal fun recordExplode(): Boolean {
        explode = when (val match = match()) {
            "" -> d!! + 1
            "!" -> d!!
            else -> match.substring(1).toInt()
        }
        return true
    }

    internal fun rollTheDice(): Boolean {
        return push(
            Roller(
                n!!,
                d!!,
                reroll!!,
                keep!!,
                explode!!,
                random,
                verbose
            ).rollDice()
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
            push(matchAdjustment()),
            applyAddOrSubtract(),
            updateRunningTotal()
        )
    )

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

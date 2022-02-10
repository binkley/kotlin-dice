@file:Suppress("MemberVisibilityCanBePrivate")

package hm.binkley.dice

import hm.binkley.dice.DieBase.ONE
import hm.binkley.dice.DieBase.ZERO
import lombok.Generated
import org.parboiled.BaseParser
import org.parboiled.Parboiled.createParser
import org.parboiled.Rule
import org.parboiled.annotations.BuildParseTree
import org.parboiled.parserunners.ReportingParseRunner
import org.parboiled.support.ParsingResult
import kotlin.random.Random

/**
 * A dice expression evaluator.
 *
 * See [kotlin dice](https://github.com/binkley/kotlin-dice)
 *
 * @see [roll]
 *
 * @todo Several parse methods use `@Generated`: they are actually covered,
 *       but JaCoCo doesn't see through Parboiled's proxying and reflection.
 *       Which functions need `@Generated` seems hit or miss
 * @todo Is this reusable?
 */
@BuildParseTree
open class DiceParser(
    private val random: Random,
    private val reporter: RollReporter,
) : BaseParser<Int>() {
    // These properties define the current roll expression.  They are mutable
    // as the parser processes the input expression a piece at a time
    private var n: Int? = null
    private var dieBase: DieBase? = null
    private var d: Int? = null
    private var reroll: Int? = null
    private var keep: Int? = null
    private var explode: Int? = null

    /** The main entry point for the parser. */
    open fun diceExpression(): Rule = Sequence(
        rollExpression(),
        maybeRollMore(),
        maybeAdjust(),
        EOI
    )

    @Generated // Lie to JaCoCo
    internal open fun rollExpression() = Sequence(
        rollCount(),
        dieShift(),
        dieSides(),
        maybeRerollLow(),
        maybeKeepFewer(),
        maybeExplode(),
        rollTheDice()
    )

    @Generated // Lie to JaCoCo
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

    @Generated // Lie to JaCoCo
    internal open fun dieShift() = Sequence(
        FirstOf(
            Ch('d'),
            Ch('D'),
            Ch('z'),
            Ch('Z')
        ),
        recordDieShift()
    )

    internal fun recordDieShift(): Boolean {
        dieBase = when (match()) {
            "d", "D" -> ONE
            else -> ZERO
        }
        return true
    }

    @Generated // Lie to JaCoCo
    internal open fun dieSides() = Sequence(
        FirstOf(
            number(),
            Ch('%')
        ),
        recordDieSides()
    )

    internal fun recordDieSides(): Boolean {
        d = when (val match = match()) {
            "%" -> 100
            else -> match.toInt()
        }
        return true
    }

    @Generated // Lie to JaCoCo
    internal open fun maybeRerollLow() = Sequence(
        Optional(
            FirstOf(
                Ch('r'),
                Ch('R')
            ),
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

    @Generated // Lie to JaCoCo
    internal open fun maybeKeepFewer() = Sequence(
        Optional(
            FirstOf(
                Ch('h'),
                Ch('H'),
                Ch('l'),
                Ch('L')
            ),
            number()
        ),
        recordKeepFewer()
    )

    internal fun recordKeepFewer(): Boolean {
        val match = match()
        keep = when {
            match.startsWith('h') || match.startsWith('H') ->
                match.substring(1).toInt()
            match.startsWith('l') || match.startsWith('L') ->
                -match.substring(1).toInt()
            else -> n!!
        }
        return true
    }

    @Generated // Lie to JaCoCo
    internal open fun maybeExplode() = Sequence(
        Optional(
            Ch('!'),
            Optional(number())
        ),
        recordExplode()
    )

    internal fun recordExplode(): Boolean {
        explode = when (val match = match()) {
            "" -> d!! + 1 // d6 explodes on 7, meaning, no exploding
            // TODO: JaCoCo is not seeing "!" getting matched
            "!" -> d!! // d6 explodes on 6
            else -> match.substring(1).toInt()
        }
        return true
    }

    internal fun rollTheDice(): Boolean {
        return push(
            Roller(
                d!!,
                dieBase!!,
                n!!,
                reroll!!,
                keep!!,
                explode!!,
                random,
                reporter
            ).rollDice()
        )
    }

    @Generated // Lie to JaCoCo
    internal open fun maybeRollMore() = ZeroOrMore(
        Sequence(
            rememberAddOrSubtract(),
            rollExpression(),
            applyAddOrSubtract(),
            updateRunningTotal()
        )
    )

    @Generated // Lie to JaCoCo
    internal open fun rememberAddOrSubtract() = Sequence(
        arithmeticWhitespace(),
        FirstOf(
            Ch('+'),
            Ch('-')
        ),
        push(matchAddOrSubtract()),
        arithmeticWhitespace()
    )

    /** See https://github.com/sirthias/parboiled/wiki/Handling-Whitespace. */
    internal open fun arithmeticWhitespace() =
        // Note that Kotlin does not have an `\f` escape
        ZeroOrMore(AnyOf(" \t\u000c"))

    internal fun matchAddOrSubtract() = if ("+" == match()) 1 else -1

    internal fun applyAddOrSubtract() = push(pop() * pop())

    internal fun updateRunningTotal() = push(pop() + pop())

    @Generated // Lie to JaCoCo
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
}

/**
 * Parses a dice expression.
 *
 * Note: an _expensive_ call: it recreates the parser each call.
 */
fun roll(
    expression: String,
    random: Random = Random,
    reporter: RollReporter,
): ParsingResult<Int> = ReportingParseRunner<Int>(
    createParser(DiceParser::class.java, random, reporter).diceExpression()
).run(expression)

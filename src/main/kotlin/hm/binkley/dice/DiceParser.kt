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

/** Parses a dice expression, rolls, and returns the result. */
fun roll(
    expression: String,
    random: Random = Random,
    reporter: RollReporter,
): ParsingResult<Int> = ReportingParseRunner<Int>(
    createParser(DiceParser::class.java, random, reporter).diceExpression()
).run(expression)

/**
 * A dice expression evaluator.
 *
 * See [kotlin dice](https://github.com/binkley/kotlin-dice)
 *
 * @see [roll]
 *
 * @todo Several parse methods use `@Generated`: they are actually covered,
 *       but JaCoCo doesn't see through Parboiled's proxying and reflection,
 *       and use of ASM to transform bytecode does not improve this. Which
 *       functions need `@Generated` seems hit or miss
 */
@BuildParseTree
open class DiceParser(
    private val random: Random, // TODO: RandomGenerator once on Java 17
    private val reporter: RollReporter,
) : BaseParser<Int>() {
    // These properties define the current roll expression.  They are mutable
    // as the parser processes the input expression a piece at a time
    private var dieBase: DieBase? = null
    private var dieSides: Int? = null
    private var diceCount: Int? = null
    private var rerollLow: Int? = null
    private var keepCount: Int? = null
    private var explodeHigh: Int? = null
    private var multiply: Int? = null

    /**
     * This is equivalent to `build()` in builder patterns.
     *
     * @todo How do unassigned parse data have a value?  This is hard to
     *       follow: default values are from deep in the parsing functions
     *       Use of `!!` is not needed if the code were more explicit on
     *       builder pattern
     */
    fun toDiceExpression() = DiceExpression(
        dieSides = dieSides!!,
        dieBase = dieBase!!,
        diceCount = diceCount!!,
        rerollLow = rerollLow!!,
        keepCount = keepCount!!,
        explodeHigh = explodeHigh!!,
        multiply = multiply!!,
    )

    /** The main entry point for the parser. */
    open fun diceExpression(): Rule = Sequence(
        ignoreWhitespace(),
        rollExpression(),
        maybeRollMore(),
        maybeAdjust(),
        ignoreWhitespace(),
        EOI
    )

    @Generated // Lie to JaCoCo
    internal open fun rollExpression() = Sequence(
        rollCount(),
        dieBase(),
        dieSide(),
        maybeRerollLow(),
        maybeKeepFewer(),
        maybeExplode(),
        maybeMultiply(),
        rollTheDice()
    )

    @Generated // Lie to JaCoCo
    internal open fun rollCount() = Sequence(
        Optional(number()),
        recordRollCount()
    )

    internal fun recordRollCount(): Boolean {
        diceCount = matchOrDefault("1").toInt()
        return true
    }

    internal open fun number() = Sequence(
        OneOrMore(CharRange('1', '9')),
        ZeroOrMore(CharRange('0', '9'))
    )

    @Generated // Lie to JaCoCo
    internal open fun dieBase() = Sequence(
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
            else -> ZERO // must be "z" or "Z" from parsing
        }
        return true
    }

    @Generated // Lie to JaCoCo
    internal open fun dieSide() = Sequence(
        FirstOf(
            number(),
            Ch('%')
        ),
        recordDieSide()
    )

    internal fun recordDieSide(): Boolean {
        dieSides = when (val match = match()) {
            "%" -> 100
            else -> match.toInt() // must be a positive integer from parsing
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
        rerollLow = when (val match = match()) {
            "" -> Int.MIN_VALUE
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
        keepCount = when {
            match.startsWith('h') || match.startsWith('H') ->
                match.substring(1).toInt()
            match.startsWith('l') || match.startsWith('L') ->
                -match.substring(1).toInt()
            else -> diceCount!!
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
        explodeHigh = when (val match = match()) {
            "" -> dieSides!! + 1 // d6 explodes on 7, meaning, no exploding
            // TODO: JaCoCo is not seeing "!" getting matched
            "!" -> dieSides!! // d6 explodes on 6
            else -> match.substring(1).toInt()
        }
        return true
    }

    @Generated // Lie to JaCoCo
    internal open fun maybeMultiply() = Sequence(
        Optional(
            FirstOf(
                Ch('*'),
                Ch('x'),
                Ch('X'),
            ),
            number()
        ),
        recordMultiply()
    )

    internal fun recordMultiply(): Boolean {
        val match = match()
        multiply = when {
            match.startsWith('*') ||
                    match.startsWith('x') || match.startsWith('X') ->
                match.substring(1).toInt()
            else -> 1 // multiply by one is idempotent
        }
        return true
    }

    internal fun rollTheDice(): Boolean {
        return push(
            Roller(toDiceExpression(), random, reporter).rollDice()
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
        ignoreWhitespace(),
        FirstOf(
            Ch('+'),
            Ch('-')
        ),
        push(matchAddOrSubtract()),
        ignoreWhitespace()
    )

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

    /** See https://github.com/sirthias/parboiled/wiki/Handling-Whitespace. */
    internal open fun ignoreWhitespace() =
        // Note that Kotlin does not have an `\f` escape
        ZeroOrMore(AnyOf("\t\u000c "))
}

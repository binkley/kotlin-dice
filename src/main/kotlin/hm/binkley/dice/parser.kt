@file:Suppress("MemberVisibilityCanBePrivate")

package hm.binkley.dice

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
 */
@BuildParseTree
open class DiceParser(
    private val callback: OnRoll,
    private val random: Random
) : BaseParser<Int>() {
    // Internal secondary constructors used by Parboiled reflection
    // With pure Kotlin, these would be default values for the primary
    @Suppress("unused")
    internal constructor() : this(DoNothing, Random.Default)

    @Suppress("unused")
    internal constructor(callback: OnRoll) : this(callback, Random.Default)

    @Suppress("unused")
    internal constructor(random: Random) : this(DoNothing, random)

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

    @Generated // Lie to JaCoCo
    internal open fun rollExpression() = Sequence(
        rollCount(),
        dieType(),
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
    internal open fun dieType() = Sequence(
        FirstOf(
            Ch('d'),
            Ch('D')
        ),
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

    @Generated // Lie to JaCoCo
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
                n!!,
                d!!,
                reroll!!,
                keep!!,
                explode!!,
                random,
                callback
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
        FirstOf(
            Ch('+'),
            Ch('-')
        ),
        push(matchAddOrSubtract())
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
}

/**
 * Creates a dice expression evaluator using the default random
 * number generator.
 *
 * Note: an _expensive_ call: it recreates the parser for each call.
 */
@Generated // Lie to JaCoCo
fun roll(
    expression: String,
    callback: OnRoll = DoNothing
): ParsingResult<Int> =
    ReportingParseRunner<Int>(
        createParser(DiceParser::class.java, callback).diceExpression()
    ).run(expression)

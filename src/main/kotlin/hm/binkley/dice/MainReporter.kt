package hm.binkley.dice

import hm.binkley.dice.DieBase.ONE
import hm.binkley.dice.DieBase.ZERO
import org.parboiled.buffers.InputBufferUtils.collectContent
import org.parboiled.support.ParsingResult

sealed class MainReporter(
    private val minimum: Int,
    private val verbose: Boolean,
) : RollReporter {
    fun display(result: ParsingResult<Int>) = with(result) {
        if (hasErrors())
            throw BadExpressionException(parseErrors)
        if (minimum > resultValue)
            throw ResultTooLowException(minimum, resultValue)

        val input = collectContent(inputBuffer)
        val expression = if (verbose) input else normalize(input)
        println(toDisplay(expression, resultValue))
    }

    companion object {
        fun new(minimum: Int, verbose: Boolean) =
            if (verbose) VerboseReporter(minimum)
            else PlainReporter(minimum)
    }

    abstract fun preRoll()

    protected abstract fun toDisplay(expression: String, roll: Int): String
}

class PlainReporter(minimum: Int) :
    MainReporter(minimum, false) {
    override fun onRoll(dice: RolledDice) = Unit
    override fun preRoll() = Unit

    override fun toDisplay(expression: String, roll: Int) =
        "$expression @|fg_green,bold $roll|@"
}

class VerboseReporter(minimum: Int) :
    MainReporter(minimum, true) {
    override fun onRoll(dice: RolledDice) = traceRolls(dice)
    override fun preRoll() = println("---")

    override fun toDisplay(expression: String, roll: Int) =
        "@|bold $expression|@ -> @|fg_green,bold $roll|@"
}

private fun traceRolls(dice: RolledDice) = with(dice) {
    val die = when (dieBase) {
        ONE -> "d$dieSides"
        ZERO -> "z$dieSides"
    }
    val message = when (this) {
        is PlainRoll -> "roll($die) -> $roll"
        is PlainReroll -> "reroll($die) -> $roll"
        is ExplodedRoll -> "!roll($die: exploded $explodeHigh) -> $roll"
        is ExplodedReroll ->
            "!reroll($die: exploded $explodeHigh) -> $roll"
        is DroppedRoll -> "drop($die) -> $roll"
    }

    println(colorScheme.string("@|faint,italic $message|@"))
}

/**
 * Normalizes expression so that non-verbose output is easily read by shell
 * scripts, etc.
 * Normal format: `<expression> <result>`.
 */
private fun normalize(expression: String) =
    expression.trim().replace("\\s*\\+\\s*".toRegex(), "+")

private fun println(message: String) =
    kotlin.io.println(colorScheme.string(message))

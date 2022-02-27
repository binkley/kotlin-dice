package hm.binkley.dice

import hm.binkley.dice.DieBase.ONE
import hm.binkley.dice.DieBase.ZERO
import lombok.Generated
import org.parboiled.buffers.InputBufferUtils.collectContent
import org.parboiled.support.ParsingResult

internal fun selectMainReporter(
    minimum: Int,
    verbose: Boolean,
): MainReporter =
    if (verbose) VerboseReporter(minimum)
    else PlainReporter(minimum)

sealed class MainReporter(private val minimum: Int) : RollReporter {
    fun display(result: ParsingResult<Int>) = with(result) {
        if (hasErrors())
            throw BadExpressionException(parseErrors)
        if (minimum > resultValue)
            throw RollTooLowException(minimum, resultValue)

        val expression = collectContent(inputBuffer).trim()
        println(toDisplay(expression, resultValue))
    }

    abstract fun preRoll()

    protected abstract fun toDisplay(expression: String, roll: Int): String
}

internal class PlainReporter(
    minimum: Int
) : MainReporter(minimum) {
    override fun onRoll(dice: RolledDice) = Unit
    override fun preRoll() = Unit

    override fun toDisplay(expression: String, roll: Int) =
        "$expression @|bold,green $roll|@"
}

@Generated // Lie to Lombok
internal class VerboseReporter(minimum: Int) : MainReporter(minimum) {
    override fun onRoll(dice: RolledDice) = verboseRolling(dice)
    override fun preRoll() = println("---")

    override fun toDisplay(expression: String, roll: Int) =
        "@|bold $expression|@ -> @|bold,green $roll|@"
}

private fun verboseRolling(dice: RolledDice) = with(dice) {
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

private fun println(message: String) =
    kotlin.io.println(colorScheme.string(message))

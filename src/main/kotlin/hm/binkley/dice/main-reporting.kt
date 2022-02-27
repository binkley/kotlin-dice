package hm.binkley.dice

import hm.binkley.dice.DieBase.ONE
import hm.binkley.dice.DieBase.ZERO
import lombok.Generated
import org.parboiled.buffers.InputBufferUtils.collectContent
import org.parboiled.errors.ErrorUtils.printParseError
import org.parboiled.errors.InvalidInputError
import org.parboiled.errors.ParseError
import org.parboiled.support.Chars.EOI
import org.parboiled.support.ParsingResult

internal class BadExpressionException(errors: List<ParseError>) :
    Exception(errors.joinToString("\n") {
        if (it is InvalidInputError) oneLinerFor(it)
        else printParseError(it)
    })

private fun oneLinerFor(error: InvalidInputError): String {
    val at = error.startIndex
    with(error.inputBuffer) {
        val char = charAt(at)
        val position = getPosition(at)
        val where = position.column
        val expression = extractLine(position.line)
        return if (EOI == char)
            "Unexpected end in '$expression'"
        else
            "Unexpected '$char' (at position $where) in '$expression'"
    }
}

internal class RollTooLowException(
    minimum: Int,
    roll: Int,
) : Exception(
    "Roll result $roll is below the minimum result of $minimum"
)

internal fun selectMainReporter(
    minimum: Int,
    verbose: Boolean
): MainReporter =
    if (verbose) VerboseReporter(minimum)
    else PlainReporter(minimum)

sealed class MainReporter(
    private val minimum: Int
) : RollReporter {
    fun display(result: ParsingResult<Int>) = with(result) {
        if (hasErrors())
            throw BadExpressionException(parseErrors)
        if (minimum > resultValue)
            throw RollTooLowException(minimum, resultValue)

        displayExpression(collectContent(inputBuffer).trim(), resultValue)
    }

    protected abstract fun displayExpression(expression: String, roll: Int)
}

internal class PlainReporter(
    minimum: Int
) : MainReporter(minimum) {
    override fun displayExpression(expression: String, roll: Int) {
        // TODO: Colorize the result with picocli
        println("$expression $roll")
    }

    override fun onRoll(action: RollAction) = Unit
}

@Generated // Lie to Lombok
internal class VerboseReporter(
    minimum: Int
) : MainReporter(minimum) {
    override fun displayExpression(expression: String, roll: Int) {
        // TODO: Colorize the result with picocli
        println("$expression -> $roll")
    }

    override fun onRoll(action: RollAction) = verboseRolling(action)
}

/** @todo Colorize when asked */
private fun verboseRolling(action: RollAction) = with(action) {
    val die = when (dieBase) {
        ONE -> "d$dieSides"
        ZERO -> "z$dieSides"
    }
    println(
        when (this) {
            is PlainRoll -> "roll($die) -> $roll"
            is PlainReroll -> "reroll($die) -> $roll"
            is ExplodedRoll -> "!roll($die: exploded $explodeHigh) -> $roll"
            is ExplodedReroll ->
                "!reroll($die: exploded $explodeHigh) -> $roll"
            is DroppedRoll -> "drop($die) -> $roll"
        }
    )
}

package hm.binkley.dice

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

internal fun selectMainReporter(
    minimum: Int,
    verbose: Boolean
): MainReporter =
    if (verbose) VerboseReporter(minimum)
    else PlainReporter(minimum)

internal class RollTooLowException(
    minimum: Int,
    roll: Int,
) : Exception(
    "Roll result $roll is below the minimum result of $minimum"
)

sealed class MainReporter(
    private val minimum: Int
) : RollReporter {
    fun display(result: ParsingResult<Int>): Unit = with(result) {
        if (result.hasErrors())
            throw BadExpressionException(parseErrors)

        displayExpression(expression, roll)
    }

    abstract fun displayExpression(expression: String, roll: Int)

    private val ParsingResult<Int>.expression: String
        get() = collectContent(inputBuffer).trim()
    private val ParsingResult<Int>.roll: Int
        get() =
            if (minimum > resultValue)
                throw RollTooLowException(minimum, resultValue)
            else resultValue
}

internal class PlainReporter(
    minimum: Int
) : MainReporter(minimum) {
    override fun displayExpression(expression: String, roll: Int) {
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
        println("RESULT -> $roll")
    }

    override fun onRoll(action: RollAction) = verboseRolling(action)
}

/** @todo Colorize when asked */
private fun verboseRolling(action: RollAction) {
    println(
        when (action) {
            is PlainRoll -> "roll(d${action.dieSides}) -> ${action.roll}"
            is PlainReroll -> "reroll(d${action.dieSides}) -> ${action.roll}"
            is ExplodedRoll -> "!roll(d${action.dieSides}: exploding on ${action.explode}) -> ${action.roll}"
            is ExplodedReroll -> "!reroll(d${action.dieSides}: exploding on ${action.explode}) -> ${action.roll}"
            is DroppedRoll -> "drop -> ${action.roll}"
        }
    )
}

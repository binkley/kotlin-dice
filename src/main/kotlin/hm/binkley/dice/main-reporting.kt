package hm.binkley.dice

import lombok.Generated
import org.parboiled.buffers.InputBufferUtils.collectContent
import org.parboiled.errors.ErrorUtils.printParseError
import org.parboiled.errors.ParseError
import org.parboiled.support.ParsingResult

internal class BadDiceExpressionException(errors: List<ParseError>) :
    Exception(errors.joinToString("\n") {
        printParseError(it)
    })

internal fun selectMainReporter(
    minimum: Int,
    verbose: Boolean,
    colored: Boolean
): MainReporter = when (verbose to colored) {
    true to true -> ColoredVerboseReporter(minimum)
    true to false -> UncoloredVerboseReporter(minimum)
    false to true -> ColoredPlainReporter(minimum)
    else /* false to false */ -> UncoloredPlainReporter(minimum)
}

class RollTooLowException(
    minimum: Int,
    roll: Int,
) : RuntimeException(
    "Roll result $roll is below the minimum result of $minimum"
)

sealed class MainReporter(
    private val minimum: Int
) : RollReporter {
    fun display(result: ParsingResult<Int>): Unit = with(result) {
        if (!result.hasErrors())
            displayExpression(expression, roll)
        throw BadDiceExpressionException(parseErrors)
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

internal class UncoloredPlainReporter(
    minimum: Int
) : MainReporter(minimum) {
    override fun displayExpression(expression: String, roll: Int) {
        println("$expression $roll")
    }

    override fun onRoll(action: RollAction) = Unit
}

@Generated // Lie to JaCoCo
internal class ColoredPlainReporter(
    minimum: Int
) : MainReporter(minimum) {
    override fun displayExpression(expression: String, roll: Int) {
        println("$expression $roll")
    }

    override fun onRoll(action: RollAction) = Unit
}

internal class UncoloredVerboseReporter(
    minimum: Int
) : MainReporter(minimum) {
    override fun displayExpression(expression: String, roll: Int) {
        println("RESULT -> $roll")
    }

    override fun onRoll(action: RollAction) = verboseRolling(action)
}

@Generated // Lie to Lombok
internal class ColoredVerboseReporter(
    minimum: Int
) : MainReporter(minimum) {
    override fun displayExpression(expression: String, roll: Int) {
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

package hm.binkley.dice

import lombok.Generated
import org.fusesource.jansi.Ansi.ansi
import org.parboiled.buffers.InputBufferUtils.collectContent
import org.parboiled.errors.ErrorUtils.printParseError
import org.parboiled.errors.ParseError
import org.parboiled.support.ParsingResult
import java.lang.System.err

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
    abstract fun display(result: ParsingResult<Int>)

    val ParsingResult<Int>.expression: String
        get() = collectContent(inputBuffer)
    val ParsingResult<Int>.roll: Int
        get() =
            if (minimum > resultValue)
                throw RollTooLowException(minimum, resultValue)
            else resultValue
}

internal class UncoloredPlainReporter(
    minimum: Int
) : MainReporter(minimum) {
    override fun onRoll(action: RollAction) = Unit

    override fun display(result: ParsingResult<Int>) {
        if (!result.hasErrors())
            println("${result.expression.trim()} ${result.roll}")
        else result.parseErrors.forEach {
            displayErrorUncolored(it)
        }
    }
}

@Generated // Lie to JaCoCo
internal class ColoredPlainReporter(
    minimum: Int
) : MainReporter(minimum) {
    override fun onRoll(action: RollAction) = Unit

    override fun display(result: ParsingResult<Int>) {
        if (!result.hasErrors())
            println("${result.expression.trim()} ${result.roll}")
        else result.parseErrors.forEach {
            displayErrorColored(it)
        }
    }
}

internal class UncoloredVerboseReporter(
    minimum: Int
) : MainReporter(minimum) {
    override fun onRoll(action: RollAction) = verboseRolling(action)

    override fun display(result: ParsingResult<Int>) {
        if (!result.hasErrors())
            println("RESULT -> ${result.roll}")
        else result.parseErrors.forEach {
            displayErrorUncolored(it)
        }
    }
}

@Generated // Lie to Lombok
internal class ColoredVerboseReporter(
    minimum: Int
) : MainReporter(minimum) {
    override fun onRoll(action: RollAction) = verboseRolling(action)

    override fun display(result: ParsingResult<Int>) {
        if (!result.hasErrors())
            println("RESULT -> ${result.roll}")
        else result.parseErrors.forEach {
            displayErrorColored(it)
        }
    }
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

private fun displayErrorUncolored(error: ParseError) =
    err.println(printParseError(error))

@Generated // Lie to Lombok
private fun displayErrorColored(error: ParseError) =
    err.println(
        ansi().bold().fgRed().a(printParseError(error)).reset().toString()
    )

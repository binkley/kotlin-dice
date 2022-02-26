package hm.binkley.dice

import lombok.Generated
import org.fusesource.jansi.Ansi.ansi
import org.parboiled.buffers.InputBufferUtils.collectContent
import org.parboiled.errors.ErrorUtils.printParseError
import org.parboiled.errors.ParseError
import org.parboiled.support.ParsingResult
import java.lang.System.err

internal fun selectMainReporter(verbose: Boolean, colored: Boolean)
        : MainReporter {
    return if (verbose) {
        if (colored) ColoredVerboseReporter
        else UncoloredVerboseReporter
    } else {
        if (colored) ColoredPlainReporter
        else UncoloredPlainReporter
    }
}

sealed interface MainReporter : RollReporter {
    fun display(result: ParsingResult<Int>)

    val ParsingResult<Int>.expression: String
        get() = collectContent(inputBuffer)
    val ParsingResult<Int>.roll: Int get() = resultValue
}

internal object UncoloredPlainReporter : MainReporter {
    override fun onRoll(action: RollAction) = Unit

    override fun display(result: ParsingResult<Int>) {
        if (!result.hasErrors())
            println("${result.expression.trim()} ${result.roll}")
        else result.parseErrors.forEach {
            displayErrorUncolored(it)
        }
    }
}

@Generated // Lie to Lombok
internal object ColoredPlainReporter : MainReporter {
    override fun onRoll(action: RollAction) = Unit

    override fun display(result: ParsingResult<Int>) {
        if (!result.hasErrors())
            println("${result.expression.trim()} ${result.roll}")
        else result.parseErrors.forEach {
            displayErrorColored(it)
        }
    }
}

internal object UncoloredVerboseReporter : MainReporter {
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
internal object ColoredVerboseReporter : MainReporter {
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
            is PlainRoll -> "roll(d${action.d}) -> ${action.roll}"
            is PlainReroll -> "reroll(d${action.d}) -> ${action.roll}"
            is ExplodedRoll -> "!roll(d${action.d}: exploding on ${action.explode}) -> ${action.roll}"
            is ExplodedReroll -> "!reroll(d${action.d}: exploding on ${action.explode}) -> ${action.roll}"
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

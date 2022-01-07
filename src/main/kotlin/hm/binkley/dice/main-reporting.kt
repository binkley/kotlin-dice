package hm.binkley.dice

import org.fusesource.jansi.Ansi.ansi
import org.parboiled.buffers.InputBufferUtils.collectContent
import org.parboiled.errors.ErrorUtils.printParseError
import org.parboiled.errors.ParseError
import org.parboiled.support.ParsingResult
import java.lang.System.err

fun selectMainReporter(verbose: Boolean, colored: Boolean): MainReporter {
    if (verbose) {
        return if (colored) ColoredVerboseReporter
        else UncoloredVerboseReporter
    } else {
        return if (colored) ColoredPlainReporter
        else UncoloredPlainReporter
    }
}

sealed interface MainReporter : RollReporter {
    fun display(result: ParsingResult<Int>)

    val ParsingResult<Int>.expression: String
        get() = collectContent(inputBuffer)
    val ParsingResult<Int>.roll: Int get() = resultValue
}

internal object FixMeMainReporter : MainReporter {
    override fun display(result: ParsingResult<Int>) =
        TODO("BROKEN LOGIC FOR main()")

    override fun onRoll(action: RollAction) =
        TODO("BROKEN LOGIC FOR main()")
}

internal object UncoloredPlainReporter : MainReporter {
    override fun onRoll(action: RollAction) = Unit

    override fun display(result: ParsingResult<Int>) {
        if (!result.hasErrors())
            println("${result.expression} ${result.roll}")
        else result.parseErrors.forEach {
            displayErrorUncolored(it)
        }
    }
}

internal object ColoredPlainReporter : MainReporter {
    override fun onRoll(action: RollAction) = Unit

    override fun display(result: ParsingResult<Int>) {
        if (!result.hasErrors())
            println("${result.expression} ${result.roll}")
        else result.parseErrors.forEach {
            displayErrorColored(it)
        }
    }
}

internal object UncoloredVerboseReporter : MainReporter {
    override fun onRoll(action: RollAction) {
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

    override fun display(result: ParsingResult<Int>) {
        if (!result.hasErrors())
            println("RESULT -> ${result.roll}")
        else result.parseErrors.forEach {
            displayErrorUncolored(it)
        }
    }
}

internal object ColoredVerboseReporter : MainReporter {
    override fun onRoll(action: RollAction) {
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

    override fun display(result: ParsingResult<Int>) {
        if (!result.hasErrors())
            println("RESULT -> ${result.roll}")
        else result.parseErrors.forEach {
            displayErrorColored(it)
        }
    }
}

private fun displayErrorUncolored(error: ParseError) =
    err.println(printParseError(error))

private fun displayErrorColored(error: ParseError) =
    err.println(
        ansi().bold().fgRed().a(printParseError(error)).reset().toString()
    )

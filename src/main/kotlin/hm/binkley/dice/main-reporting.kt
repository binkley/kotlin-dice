package hm.binkley.dice

import org.fusesource.jansi.Ansi.ansi
import org.parboiled.buffers.InputBufferUtils.collectContent
import org.parboiled.errors.ErrorUtils.printParseError
import org.parboiled.errors.ParseError
import org.parboiled.support.ParsingResult
import java.lang.System.err

sealed interface MainReporter : RollReporter {
    fun display(result: ParsingResult<Int>)

    val ParsingResult<Int>.expression: String
        get() = collectContent(inputBuffer)
    val ParsingResult<Int>.roll: Int get() = resultValue
}

internal class PlainReporter(
    private val withColor: Boolean,
) : MainReporter {
    override fun onRoll(action: RollAction) = Unit

    override fun display(result: ParsingResult<Int>) {
        if (!result.hasErrors())
            println("${result.expression} ${result.roll}")
        else result.parseErrors.forEach {
            displayError(withColor, it)
        }
    }
}

internal class VerboseReporter(
    private val withColor: Boolean,
) : MainReporter {
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
            displayError(withColor, it)
        }
    }
}

private fun displayError(withColor: Boolean, it: ParseError) {
    val errorText = printParseError(it)
    val errorDisplay =
        if (!withColor) errorText
        else ansi().bold().fgRed().a(errorText).reset().toString()
    err.println(errorDisplay)
}

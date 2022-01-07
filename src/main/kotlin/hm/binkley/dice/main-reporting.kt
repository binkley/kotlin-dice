package hm.binkley.dice

import org.parboiled.buffers.InputBufferUtils.collectContent
import org.parboiled.errors.ErrorUtils.printParseError
import org.parboiled.support.ParsingResult
import java.lang.System.err

sealed interface MainReporter : RollReporter {
    fun display(result: ParsingResult<Int>)

    val ParsingResult<Int>.expression: String
        get() = collectContent(inputBuffer)
    val ParsingResult<Int>.roll: Int get() = resultValue
}

internal object PlainReporter : MainReporter {
    override fun onRoll(action: RollAction) = Unit

    override fun display(result: ParsingResult<Int>) {
        if (!result.hasErrors())
            println("${result.expression} ${result.roll}")
        else result.parseErrors.forEach {
            err.println(printParseError(it))
        }
    }
}

internal object VerboseReporter : MainReporter {
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
            err.println(printParseError(it))
        }
    }
}

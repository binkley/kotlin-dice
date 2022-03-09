package hm.binkley.dice

import org.jline.reader.UserInterruptException
import picocli.CommandLine
import picocli.CommandLine.IExecutionExceptionHandler
import picocli.CommandLine.IExecutionStrategy
import picocli.CommandLine.RunLast
import kotlin.system.exitProcess

const val PROGRAM_NAME = "roll"
const val COLORFUL_DIE_PROMPT = "\uD83C\uDFB2 "

fun main(args: Array<String>) {
    val options = Options()

    exitProcess(
        CommandLine(options)
            .setExecutionExceptionHandler(options.exceptionHandler())
            .setExecutionStrategy(options.executionStrategy())
            // Use the last setting for an option if it is repeated; needed
            // testing which defaults to no color, but some tests will
            // override the option to test color output
            .setOverwrittenOptionsAllowed(true)
            .execute(*args)
    )
}

fun Options.exceptionHandler() =
    IExecutionExceptionHandler { ex, commandLine, _ ->
        when (ex) {
            // User-friendly error message
            is DiceException -> {
                if (debug) commandLine.err.println(
                    colorScheme.richStackTraceString(ex)
                )
                else commandLine.err.println(
                    colorScheme.errorText(ex.message.maybeGnuPrefix())
                )
                commandLine.commandSpec.exitCodeOnExecutionException() // 1
            }
            // Special case for the REPL - shells return 130 on SIGINT
            is UserInterruptException -> 130
            // Unknown exceptions fall back to Picolo default handling
            else -> throw ex
        }
    }

/**
 * Used by both demo and testing.
 * The second value is the expectation given a seed of 1 used in testing.
 */
val demoExpressions = arrayOf(
    "d6" to 4 to "standard die",
    " d6" to 4 to "whitespace",
    "d6 " to 4 to "whitespace",
    " d6 " to 4 to "whitespace",
    "D6" to 4 to "case-insensitive",
    "z6" to 3 to "zero-based die",
    "Z6" to 3 to "case-insensitive",
    "d%" to 66 to "percentile die",
    "3d6" to 10 to "multiple dice",
    "1d1" to 1 to "bounds",
    "1z1" to 0 to " bounds",
    "d6x2" to 8 to "doubling",
    "d6X2" to 8 to "case-insensitive",
    "d6*2" to 8 to "synonym",
    "3d6 +2d4" to 17 to "whitespace",
    "3d6+ 2d4" to 17 to "whitespace",
    "3d6 + 2d4" to 17 to "whitespace",
    "3d6+1" to 11 to "adding adjustment",
    "3d6+ 1" to 11 to "whitespace",
    "3d6 +1" to 11 to "whitespace",
    "3d6 + 1" to 11 to "whitespace",
    "3d6x2+1" to 21 to "double before adding one",
    "3d6-1" to 9 to "subtracting adjustment",
    "4d6h3" to 10 to "keep 3 highest",
    "4d6H3" to 10 to "case-insensitive",
    "2d20h" to 17 to "default keep 1 highest",
    "4d6m2" to 5 to "keep 3 low middle (even/even)",
    "4d6m3" to 6 to "keep 3 low middle (even/odd)",
    "5d6m2" to 5 to "keep 3 low middle (odd/even)",
    "5d6m3" to 9 to "keep 3 low middle (odd/odd)",
    "4d6M2" to 5 to "case-insensitive",
    "2d20m" to 6 to "default keep 1 low middle",
    "4d6n2" to 5 to "keep 3 high middle (even/even)",
    "4d6n3" to 10 to "keep 3 high middle (even/odd)",
    "5d6n2" to 8 to "keep 3 high middle (odd/even)",
    "5d6n3" to 9 to "keep 3 high middle (odd/odd)",
    "4d6N2" to 5 to "case-insensitive",
    "2d20n" to 17 to "default keep 1 high middle",
    "4d6l3" to 6 to "keep 3 lowest",
    "4d6L3" to 6 to "case-insensitive",
    "2d20l" to 6 to "default keep 1 lowest",
    "10d3!" to 20 to "explode",
    "10d3!2" to 49 to "explode on value",
    "6d4l5!" to 20 to "explode with keep low",
    "3d3r1h2!" to 10 to "explode with reroll and keep high",
    "10d3!x2" to 40 to "double after exploding",
    "100d3r1h99!+100d3r1l99!3-17" to 919 to "complex",
    "d1!" to null to "explosion too low",
    "blah" to null to "syntax error",
)

private fun Options.executionStrategy() = IExecutionStrategy { parseResult ->
    // Run here rather than in Options so that --help respects the option
    color.install()

    RunLast().execute(parseResult)
}

private fun String?.maybeGnuPrefix(): String {
    val interactive = null != System.console()
    // Be careful with null handling: an NPE will have no error message.
    // GNU standards prefix error messages with program name to aid in
    // debugging pipelines, etc.
    val self = this ?: ""
    return if (interactive) self else "$PROGRAM_NAME: $self"
}

private infix fun <A, B, C> Pair<A, B>.to(third: C) =
    Triple(first, second, third)

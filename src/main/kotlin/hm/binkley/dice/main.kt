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
            .setExecutionExceptionHandler(exceptionsFor(options))
            .setExecutionStrategy(executionFor(options))
            // Use last value for repeated options (ie, `--color`)
            .setOverwrittenOptionsAllowed(true)
            .execute(*args)
    )
}

fun exceptionsFor(options: Options) =
    IExecutionExceptionHandler { ex, commandLine, _ ->
        when (ex) {
            // User-friendly error message
            is DiceException -> {
                if (options.debug) commandLine.err.println(
                    colorScheme.richStackTraceString(ex)
                )
                else commandLine.err.println(
                    colorScheme.errorText(maybeGnuPrefix() + ex.message)
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
    "4d6h3" to 10 to "keep 3 highest rolls",
    "4d6H3" to 10 to "case-insensitive",
    "4d6l3" to 6 to "keep 3 lowest rolls",
    "4d6L3" to 6 to "case-insensitive",
    "10d3!" to 20 to "explode",
    "10d3!2" to 49 to "explode on value",
    "6d4l5!" to 20 to "explode with keep low",
    "3d3r1h2!" to 10 to "explode with reroll and keep high",
    "10d3!x2" to 40 to "double after exploding",
    "100d3r1h99!+100d3r1l99!3-17" to 919 to "complex",
    "blah" to null to "syntax error",
)

private fun maybeGnuPrefix(): String {
    val interactive = null != System.console()
    // GNU standards prefix errors with program name to aid in
    // debugging failed scripts, etc.
    return if (interactive) "" else "$PROGRAM_NAME: "
}

private fun executionFor(options: Options) =
    IExecutionStrategy special@{ parseResult ->
        // Run here rather than in Options so that --help respects the option
        options.color.install()

        RunLast().execute(parseResult)
    }

private infix fun <A, B, C> Pair<A, B>.to(third: C) =
    Triple(first, second, third)

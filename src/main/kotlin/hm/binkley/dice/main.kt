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
    val processSpecialOptions = IExecutionStrategy special@{ parseResult ->
        // TODO: How to get Picocli to do these for me?
        if (options.copyright) {
            options.javaClass
                .classLoader
                .getResourceAsStream("META-INF/LICENSE")!!
                .copyTo(System.out)
            return@special 0
        }

        // Run here rather than in Options so that --help respects the flag
        options.color.install()

        RunLast().execute(parseResult)
    }

    exitProcess(
        CommandLine(options)
            .setExecutionExceptionHandler(exceptionHandling)
            .setExecutionStrategy(processSpecialOptions)
            // Use last value for repeated options (ie, `--color`)
            .setOverwrittenOptionsAllowed(true)
            .execute(*args)
    )
}

val exceptionHandling = IExecutionExceptionHandler { ex, commandLine, _ ->
    when (ex) {
        // User-friendly error message
        is DiceException -> {
            if (commandLine.getCommand<Options>().debug)
                commandLine.err.println(colorScheme.richStackTraceString(ex))
            else {
                val interactive = null != System.console()
                // GNU standards prefix errors with program name to aid in
                // debugging failed scripts, etc.
                val prefix = if (interactive) "" else "$PROGRAM_NAME: "
                commandLine.err.println(
                    colorScheme.errorText(prefix + ex.message)
                )
            }
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
    "d6" to 4,
    " d6" to 4, // whitespace
    "d6 " to 4, // whitespace
    " d6 " to 4, // whitespace
    "D6" to 4,
    "d6x2" to 8,
    "z6" to 3,
    "Z6" to 3,
    "z6x2" to 6,
    "1d1" to 1,
    "1z1" to 0,
    "3d6" to 10,
    "3D6" to 10,
    "1d1" to 1, // check bounds
    "1z1" to 0, // check bounds
    "3d6+1" to 11, // adding adjustment
    "3d6+ 1" to 11, // whitespace
    "3d6 +1" to 11, // whitespace
    "3d6 + 1" to 11, // whitespace
    "3d6x2+1" to 21, // double before adding one
    "3d6-1" to 9, // subtracting adjustment
    "10d3!" to 20,
    "10d3!*2" to 40, // double after exploding
    "10d3!x2" to 40, // synonym for times
    "10d3!X2" to 40, // synonym for times
    "10d3!2" to 49,
    "4d6h3" to 10,
    "4d6H3" to 10,
    "4d6l3" to 6,
    "4d6L3" to 6,
    "3d6+2d4" to 17,
    "3d6 +2d4" to 17, // whitespace
    "3d6+ 2d4" to 17, // whitespace
    "3d6 + 2d4" to 17, // whitespace
    "d%" to 66,
    // Constant seed keeps the roll constant, so z% is one less than d%
    "z%" to 65,
    "6d4l5!" to 20,
    "3d3r1h2!" to 10,
    "3d12!10" to 23,
    "100d3r1h99!+100d3r1l99!3-17" to 919,
    "blah" to null,
)

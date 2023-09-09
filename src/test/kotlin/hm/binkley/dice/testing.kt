package hm.binkley.dice

import com.github.stefanbirkner.systemlambda.SystemLambda.*
import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import picocli.CommandLine.Help.Ansi
import java.io.Console
import java.io.PrintWriter
import java.io.Reader.nullReader
import java.io.Writer.nullWriter
import kotlin.random.Random

internal const val FIXED_SEED = 1L

/**
 * Recreate a new `Random` for each test so results are stable and
 * independent of the order tests run; avoid reusing the _same_ `Random`
 * object.
 */
internal fun stableSeedForTesting() = Random(FIXED_SEED)

internal fun mainWithFixedSeed(vararg cmdLine: String) = main(
    arrayOf(
        "--color=never", // Default no color; enable explicitly in tests
        "--seed=$FIXED_SEED", // Hard-coded for reproducibility
        *cmdLine,
    )
)

internal inline val String.colored get() = Ansi.ON.string(this)

internal infix fun String.shouldBeAfterTrimming(expected: String) =
    trimIndent().trim() shouldBe expected.trimIndent().trim()

/**
 * **NB** &mdash; Nested system-lambda handling is needed as `main` calls
 * `System.exit`, hence assertions on `System.out` and `System.err` must come
 * _before_ trapping `System.exit`.; otherwise the exit bubbles out, and the
 * stream assertions do not run
 */
internal fun captureExecute(main: () -> Unit): ShellOutcome {
    var exitCode = -1
    var stdout = "BUG in test method"
    val stderr: String = tapSystemErrNormalized {
        stdout = tapSystemOutNormalized {
            // Undo any fiddling with color between tests
            withEnvironmentVariable("picocli.ansi", null).execute {
                exitCode = catchSystemExit(main)
            }
        }
    }

    return when (exitCode) {
        0, 1 -> ShellOutcome(exitCode, stdout, stderr)
        2 -> fail("BUG: Bad options for main()")
        else -> fail("BUG: Unexpected exit code: $exitCode")
    }
}

internal fun captureExecuteWithInput(
    vararg lines: String,
    main: () -> Unit,
): ShellOutcome {
    var outcome = ShellOutcome(-1, "BUG", "BUG")
    withTextFromSystemIn(*lines).execute {
        outcome = captureExecute(main)
    }
    return outcome
}

internal data class ShellOutcome(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
)

internal fun runWithEofConsole(block: () -> Unit) {
    mockkStatic(System::class) {
        every { System.console() } returns eofConsole
        block()
    }
}

private val eofConsole = run {
    val mockConsole = mockk<Console>()
    every { mockConsole.reader() } returns nullReader()
    every { mockConsole.writer() } returns PrintWriter(nullWriter())
    mockConsole
}

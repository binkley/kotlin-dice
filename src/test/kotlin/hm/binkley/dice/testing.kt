package hm.binkley.dice

import com.github.stefanbirkner.systemlambda.SystemLambda.catchSystemExit
import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErrNormalized
import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOutNormalized
import com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable
import com.github.stefanbirkner.systemlambda.SystemLambda.withTextFromSystemIn
import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import picocli.CommandLine.Help.Ansi
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
        "--color=never", // Force color off for testing
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
internal fun captureRun(main: () -> Unit): ShellOutcome {
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

internal fun captureRunWithInput(
    vararg lines: String,
    main: () -> Unit,
): ShellOutcome {
    var outcome = ShellOutcome(-1, "BUG", "BUG")
    withTextFromSystemIn(*lines).execute {
        outcome = captureRun(main)
    }
    return outcome
}

internal data class ShellOutcome(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
)

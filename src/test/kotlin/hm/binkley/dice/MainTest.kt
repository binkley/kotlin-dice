package hm.binkley.dice

import com.github.stefanbirkner.systemlambda.SystemLambda.catchSystemExit
import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErrNormalized
import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOutNormalized
import com.github.stefanbirkner.systemlambda.SystemLambda.withTextFromSystemIn
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldNotBeEmpty
import org.junit.jupiter.api.Test

internal class MainTest {
    @Test
    fun `should show basic help`() {
        val (exitCode, out, err) = runWithCapture {
            testMain("--help")
        }

        exitCode shouldBe 0
        out.shouldNotBeEmpty()
        err.shouldBeEmpty()
    }

    @Test
    fun `should show software version`() {
        val (exitCode, out, err) = runWithCapture {
            testMain("--version")
        }

        exitCode shouldBe 0
        out.shouldNotBeEmpty()
        err.shouldBeEmpty()
    }

    @Test
    fun `should roll dice with a default RNG`() {
        val (exitCode, out, err) = runWithCapture {
            main(arrayOf("3d6")) // Avoid testing seed
        }

        exitCode shouldBe 0
        out.shouldNotBeEmpty()
        err.shouldBeEmpty()
    }

    @Test
    fun `should roll dice from command line`() {
        val (exitCode, out, err) = runWithCapture {
            testMain("3d6")
        }

        exitCode shouldBe 0
        out shouldBeAfterStripping "3d6 10"
        err.shouldBeEmpty()
    }

    @Test
    fun `should roll dice from command line in color`() {
        val (exitCode, out, err) = runWithCapture {
            testMain("--color", "3d6")
        }

        exitCode shouldBe 0
        out shouldBeAfterStripping "3d6 10"
        err.shouldBeEmpty()
    }

    @Test
    fun `should roll dice from command line verbosely and in color`() {
        val (exitCode, out, err) = runWithCapture {
            testMain("--verbose", "--color", "3d6")
        }

        exitCode shouldBe 0
        out shouldBeAfterStripping """
roll(d6) -> 4
roll(d6) -> 1
roll(d6) -> 5
RESULT -> 10
"""
        err.shouldBeEmpty()
    }

    @Test
    fun `should fail if command line is bad`() {
        val (exitCode, out, err) = runWithCapture {
            testMain("3d6", "x")
        }

        exitCode shouldBe 1
        out shouldBeAfterStripping "3d6 10"
        err shouldBeAfterStripping """
Invalid input 'x', expected diceExpression (line 1, pos 1):
x
^
"""
    }

    @Test
    fun `should roll dice from STDIN`() {
        // TODO: This is ugly needing to hack the environment for testing :(
//        withEnvironmentVariable("TERM", "dumb").execute {
        withTextFromSystemIn("3d6").execute {
            val (exitCode, out, err) = runWithCapture {
                testMain()
            }

            exitCode shouldBe 0
            out shouldBeAfterStripping "3d6 10"
            err.shouldBeEmpty()
        }
    }

    @Test
    fun `should do nothing if STDIN is empty`() {
        withTextFromSystemIn().execute {
            val err = tapSystemErrNormalized {
                val out = tapSystemOutNormalized {
                    val exitCode = catchSystemExit {
                        testMain()
                    }
                    exitCode shouldBe 0
                }
                out.shouldBeEmpty()
            }
            err.shouldBeEmpty()
        }
    }

    @Test
    fun `should do nothing if STDIN is just a blank line`() {
        withTextFromSystemIn("").execute {
            val (exitCode, out, err) = runWithCapture {
                testMain()
            }

            exitCode shouldBe 0
            out.shouldBeEmpty()
            err.shouldBeEmpty()
        }
    }

    @Test
    fun `should fail if STDIN is bad`() {
        withTextFromSystemIn("3d6", "x").execute {
            val (exitCode, out, err) = runWithCapture {
                testMain()
            }

            exitCode shouldBe 1
            out shouldBeAfterStripping "3d6 10"
            err shouldBeAfterStripping """
Invalid input 'x', expected diceExpression (line 1, pos 1):
x
^
"""
        }
    }

    @Test
    fun `should run demo`() {
        val (exitCode, out, err) = runWithCapture {
            testMain("--demo")
        }

        exitCode shouldBe 0
        out.shouldEndWith("DONE\n")
        err.shouldNotBeEmpty()
    }

    @Test
    fun `should run demo verbosely`() {
        val (exitCode, out, err) = runWithCapture {
            testMain("--demo", "--verbose")
        }

        exitCode shouldBe 0
        out.shouldEndWith("DONE\n")
        err.shouldNotBeEmpty()
    }

    @Test
    fun `should run demo in color`() {
        val (exitCode, out, err) = runWithCapture {
            testMain("--demo", "--color")
        }

        exitCode shouldBe 0
        out.shouldEndWith("DONE\n")
        err.shouldNotBeEmpty()
    }

    @Test
    fun `should run demo verbosely and in color`() {
        val (exitCode, out, err) = runWithCapture {
            testMain("--demo", "--color", "--verbose")
        }

        exitCode shouldBe 0
        out.shouldEndWith("DONE\n")
        err.shouldNotBeEmpty()
    }
}

private fun testMain(vararg cmdLine: String) = main(
    arrayOf(
        "--seed=${TESTING_SEED}", // Hard-coded for reproducibility
        *cmdLine,
    )
)

/** @todo Kotlin portable equivalent of `strip()` */
private infix fun String.shouldBeAfterStripping(expected: String) =
    trimIndent().strip() shouldBe expected.trimIndent().strip()

/**
 * **NB** &mdash; Nested system-lambda handling is needed as `main` calls
 * `System.exit`, hence assertions on `System.out` and `System.err` must come
 * _before_ trapping `System.exit`.; otherwise the exit bubbles out, and the
 * stream assertions do not run
 */
private fun runWithCapture(main: () -> Unit): ShellOutcome {
    var exitCode = -1
    var stdout = "BUG in test method"
    val stderr: String = tapSystemErrNormalized {
        stdout = tapSystemOutNormalized {
            exitCode = catchSystemExit(main)
        }
    }

    return ShellOutcome(exitCode, stdout, stderr)
}

private data class ShellOutcome(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
)

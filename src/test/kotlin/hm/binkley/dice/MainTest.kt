package hm.binkley.dice

import com.github.stefanbirkner.systemlambda.SystemLambda.catchSystemExit
import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErrNormalized
import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOutNormalized
import com.github.stefanbirkner.systemlambda.SystemLambda.withTextFromSystemIn
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldNotBeEmpty
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class MainTest {
    @Nested
    inner class BasicOptions {
        @Test
        fun `should show basic help`() {
            val (exitCode, out, err) = runWithCapture {
                mainWithFixedSeed("--help")
            }

            exitCode shouldBe 0
            out.shouldNotBeEmpty()
            err.shouldBeEmpty()
        }

        @Test
        fun `should show software version`() {
            val (exitCode, out, err) = runWithCapture {
                mainWithFixedSeed("--version")
            }

            exitCode shouldBe 0
            out.shouldNotBeEmpty()
            err.shouldBeEmpty()
        }
    }

    @Nested
    inner class DefaultMain {
        @Test
        fun `should roll dice with a default RNG`() {
            val (exitCode, out, err) = runWithCapture {
                main(arrayOf("3d6")) // Avoid the testing seed
            }

            exitCode shouldBe 0
            out.shouldNotBeEmpty()
            err.shouldBeEmpty()
        }
    }

    @Nested
    inner class CommandLine {
        @Test
        fun `should roll dice from command line`() {
            val (exitCode, out, err) = runWithCapture {
                mainWithFixedSeed("3d6")
            }

            exitCode shouldBe 0
            out shouldBeAfterTrimming "3d6 10"
            err.shouldBeEmpty()
        }

        @Test
        fun `should roll dice from command line in color`() {
            val (exitCode, out, err) = runWithCapture {
                mainWithFixedSeed("--color", "3d6")
            }

            exitCode shouldBe 0
            out shouldBeAfterTrimming "3d6 10"
            err.shouldBeEmpty()
        }

        @Test
        fun `should roll dice from command line verbosely and in color`() {
            val (exitCode, out, err) = runWithCapture {
                mainWithFixedSeed("--verbose", "--color", "3d6")
            }

            exitCode shouldBe 0
            out shouldBeAfterTrimming """
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
                mainWithFixedSeed("3d6", "x")
            }

            exitCode shouldBe 1
            out shouldBeAfterTrimming "3d6 10"
            err shouldBeAfterTrimming """
Invalid input 'x', expected diceExpression (line 1, pos 1):
x
^
"""
        }
    }

    @Nested
    inner class StandardInput {
        @Test
        fun `should roll dice from STDIN`() {
            // TODO: This is ugly needing to hack the environment for testing :(
//        withEnvironmentVariable("TERM", "dumb").execute {
            withTextFromSystemIn("3d6").execute {
                val (exitCode, out, err) = runWithCapture {
                    mainWithFixedSeed()
                }

                exitCode shouldBe 0
                out shouldBeAfterTrimming "3d6 10"
                err.shouldBeEmpty()
            }
        }

        @Test
        fun `should do nothing if STDIN is empty`() {
            withTextFromSystemIn().execute {
                val err = tapSystemErrNormalized {
                    val out = tapSystemOutNormalized {
                        val exitCode = catchSystemExit {
                            mainWithFixedSeed()
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
                    mainWithFixedSeed()
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
                    mainWithFixedSeed()
                }

                exitCode shouldBe 1
                out shouldBeAfterTrimming "3d6 10"
                err shouldBeAfterTrimming """
Invalid input 'x', expected diceExpression (line 1, pos 1):
x
^
"""
            }
        }
    }

    @Nested
    inner class Demo {
        @Test
        fun `should run demo`() {
            val (exitCode, out, err) = runWithCapture {
                mainWithFixedSeed("--demo")
            }

            exitCode shouldBe 0
            out.shouldEndWith("DONE\n")
            err.shouldNotBeEmpty()
        }

        @Test
        fun `should run demo verbosely`() {
            val (exitCode, out, err) = runWithCapture {
                mainWithFixedSeed("--demo", "--verbose")
            }

            exitCode shouldBe 0
            out.shouldEndWith("DONE\n")
            err.shouldNotBeEmpty()
        }

        @Test
        fun `should run demo in color`() {
            val (exitCode, out, err) = runWithCapture {
                mainWithFixedSeed("--demo", "--color")
            }

            exitCode shouldBe 0
            out.shouldEndWith("DONE\n")
            err.shouldNotBeEmpty()
        }

        @Test
        fun `should run demo verbosely and in color`() {
            val (exitCode, out, err) = runWithCapture {
                mainWithFixedSeed("--demo", "--color", "--verbose")
            }

            exitCode shouldBe 0
            out.shouldEndWith("DONE\n")
            err.shouldNotBeEmpty()
        }
    }

    @Nested
    inner class Whitespace {
        @Test
        fun `should trim dice expression`() {
            val (exitCode, out, err) = runWithCapture {
                mainWithFixedSeed(" 3d6 + 1 ")
            }

            exitCode shouldBe 0
            // NOT trimmed by test.  If main broken, might be:
            // - " 3d6 + 1  11\n"
            // - "3d6+1 11\n"
            out shouldBe "3d6 + 1 11\n"
            err.shouldBeEmpty()
        }
    }
}

private fun mainWithFixedSeed(vararg cmdLine: String) = main(
    arrayOf(
        "--seed=${FIXED_SEED}", // Hard-coded for reproducibility
        *cmdLine,
    )
)

private infix fun String.shouldBeAfterTrimming(expected: String) =
    trimIndent().trim() shouldBe expected.trimIndent().trim()

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

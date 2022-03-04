package hm.binkley.dice

import com.github.stefanbirkner.systemlambda.SystemLambda.catchSystemExit
import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErrNormalized
import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOutNormalized
import com.github.stefanbirkner.systemlambda.SystemLambda.withTextFromSystemIn
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.jline.reader.UserInterruptException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import picocli.CommandLine.Command
import picocli.CommandLine.Model.CommandSpec
import picocli.CommandLine.ParseResult

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
            out shouldBeAfterTrimming """
3d6 10
"""
            err.shouldBeEmpty()
        }

        @Test
        fun `should roll dice from command line in color`() {
            val (exitCode, out, err) = runWithCapture {
                // Force color with option parameter
                mainWithFixedSeed("--color=always", "3d6")
            }

            exitCode shouldBe 0
            out shouldBeAfterTrimming """
3d6 @|fg_green,bold 10|@
""".colored
            err.shouldBeEmpty()
        }

        @Test
        fun `should roll dice from command line verbosely and in color`() {
            val (exitCode, out, err) = runWithCapture {
                // Force color with fallback 'always' parameter value
                mainWithFixedSeed("-C", "--verbose", "3d6")
            }

            exitCode shouldBe 0
            out shouldBeAfterTrimming """
---
@|faint,italic roll(d6) -> 4|@
@|faint,italic roll(d6) -> 1|@
@|faint,italic roll(d6) -> 5|@
@|bold 3d6|@ -> @|fg_green,bold 10|@
""".colored
            err.shouldBeEmpty()
        }
    }

    @Nested
    inner class Errors {
        @Test
        fun `should fail gnuishly with 1-liner for bad expression`() {
            val (exitCode, out, err) = runWithCapture {
                mainWithFixedSeed("3d6", "3dd")
            }

            exitCode shouldBe 1
            out shouldBeAfterTrimming """
3d6 10
"""
            err shouldBeAfterTrimming """
roll: Unexpected 'd' (at position 3) in dice expression '3dd'
"""
        }

        @Test
        fun `should fail gnuishly with 1-liner for incomplete expression`() {
            val (exitCode, out, err) = runWithCapture {
                mainWithFixedSeed("3d6", "3d")
            }

            exitCode shouldBe 1
            out shouldBeAfterTrimming """
3d6 10
"""
            err shouldBeAfterTrimming """
roll: Incomplete dice expression '3d'
"""
        }

        @Test
        fun `should fail in color`() {
            val (exitCode, out, err) = runWithCapture {
                mainWithFixedSeed("--color=always", "3d6", "3d")
            }

            exitCode shouldBe 1
            out shouldBeAfterTrimming """
3d6 @|fg_green,bold 10|@
""".colored
            // NB -- order of fg_red,bold and bold,fg_red matters
            err shouldBeAfterTrimming """
@|fg_red,bold Incomplete dice expression '3d'|@
""".colored
        }

        @Test
        fun `should fail debuggingly with stack trace`() {
            val (exitCode, out, err) = runWithCapture {
                mainWithFixedSeed("--debug", "3d6", "3d")
            }

            exitCode shouldBe 1
            out shouldBeAfterTrimming """
---
roll(d6) -> 4
roll(d6) -> 1
roll(d6) -> 5
3d6 -> 10
---                
"""
            // TODO: assertion is sensitive to MainReporter line numbers
            err.shouldStartWith("""
hm.binkley.dice.BadExpressionException: Incomplete dice expression '3d'
	at hm.binkley.dice.MainReporter.display(MainReporter.kt:14)
""".trimIndent()
            )
        }

        @Command
        inner class Immaterial

        private val commandLine = picocli.CommandLine(Immaterial())
        private val parseResult =
            ParseResult.builder(CommandSpec.create()).build()

        @Test
        fun `should exit on interrupt the same as shells`() {
            val exitCode = exceptionHandling.handleExecutionException(
                UserInterruptException("I was typing somethi^C"),
                commandLine,
                parseResult,
            )

            exitCode shouldBe 130
        }

        @Test
        fun `should let framework handle unknown exceptions`() {
            @Command
            class Immaterial

            val ex = NullPointerException("Did I forget something?")
            val thrown = shouldThrow<NullPointerException> {
                exceptionHandling.handleExecutionException(
                    ex,
                    commandLine,
                    parseResult,
                )
            }

            thrown shouldBeSameInstanceAs ex
        }
    }

    @Nested
    inner class StandardInput {
        @Test
        fun `should roll dice from STDIN`() {
            withTextFromSystemIn("3d6").execute {
                val (exitCode, out, err) = runWithCapture {
                    mainWithFixedSeed()
                }

                exitCode shouldBe 0
                out shouldBeAfterTrimming """
3d6 10
"""
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
                // Force color with option parameter
                mainWithFixedSeed("--demo", "--color=always")
            }

            exitCode shouldBe 0
            out.shouldEndWith("@|bold DONE|@\n".colored)
            err.shouldNotBeEmpty()
        }

        @Test
        fun `should run demo verbosely and in color`() {
            val (exitCode, out, err) = runWithCapture {
                // Force color with fallback 'always' parameter value
                mainWithFixedSeed("-C", "--demo", "--verbose")
            }

            exitCode shouldBe 0
            out.shouldEndWith("@|bold DONE|@\n".colored)
            err.shouldNotBeEmpty()
        }
    }

    @Nested
    inner class Outputs {
        @Test
        fun `should normalize result output`() {
            val (exitCode, out, err) = runWithCapture {
                mainWithFixedSeed(
                    "3d6+1",
                    " 3d6+1",
                    "3d6+1 ",
                    "3d6 +1",
                    "3d6+ 1",
                    "3d6 + 1",
                    " 3d6 + 1 ",
                )
            }

            exitCode shouldBe 0
            out shouldBeAfterTrimming """
3d6+1 11
3d6+1 12
3d6+1 10
3d6+1 9
3d6+1 13
3d6+1 13
3d6+1 19
"""
            err.shouldBeEmpty()
        }

        @Test
        fun `should not normalize result output when verbose`() {
            val (exitCode, out, err) = runWithCapture {
                mainWithFixedSeed("--verbose", " 1d1 + 1 ")
            }

            exitCode shouldBe 0
            out shouldBeAfterTrimming """
---
roll(d1) -> 1
 1d1 + 1  -> 2
"""
            err.shouldBeEmpty()
        }
    }

    @Nested
    inner class MinimalRolls {
        @Test
        fun `should roll below 0`() {
            val (exitCode, out, err) = runWithCapture {
                mainWithFixedSeed("1z1-1")
            }

            exitCode shouldBe 0
            out shouldBeAfterTrimming """
1z1-1 -1
"""
            err.shouldBeEmpty()
        }

        @Test
        fun `should fail below 0`() {
            val (exitCode, out, err) = runWithCapture {
                mainWithFixedSeed("--minimum=0", "1z1-1")
            }

            exitCode shouldBe 1
            out.shouldBeEmpty()
            err shouldBeAfterTrimming """
roll: Roll result -1 is below the minimum result of 0
"""
            err.shouldNotBeEmpty()
        }
    }
}

private fun mainWithFixedSeed(vararg cmdLine: String) = main(
    arrayOf(
        "--color=never", // Force color off for testing
        "--seed=$FIXED_SEED", // Hard-coded for reproducibility
        *cmdLine,
    )
)

private inline val String.colored get() = colorScheme.string(this)

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

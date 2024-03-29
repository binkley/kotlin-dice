package hm.binkley.dice

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.*
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.jline.reader.UserInterruptException
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import picocli.CommandLine.Model.CommandSpec
import picocli.CommandLine.ParseResult

internal class MainTest {
    @Nested
    inner class BasicOptions {
        @Test
        fun `should show help`() {
            val (exitCode, out, err) = capture {
                withFixedDiceRolls("--help")
            }

            exitCode shouldBe 0
            out shouldContain "\nDescription:\n"
            err.shouldBeEmpty()
        }

        @Test
        fun `should show help in color when forced`() {
            val (exitCode, out, err) = capture {
                withFixedDiceRolls("--color=always", "--help")
            }

            exitCode shouldBe 0
            out shouldContain "\n@|bold,underline Description:|@\n".colored
            err.shouldBeEmpty()
        }

        @Test
        fun `should show software version`() {
            val (exitCode, out, err) = capture {
                withFixedDiceRolls("--version")
            }

            exitCode shouldBe 0
            out.shouldNotBeEmpty()
            err.shouldBeEmpty()
        }

        @Test
        fun `should show copyright`() {
            val (exitCode, out, err) = capture {
                withFixedDiceRolls("--copyright")
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
            val (exitCode, out, err) = capture {
                main(arrayOf("3d6")) // Avoid the testing seed
            }

            exitCode shouldBe 0
            out.shouldNotBeEmpty()
            err.shouldBeEmpty()
        }
    }

    @Nested
    inner class Arguments {
        @Test
        fun `should roll dice from command line`() {
            val (exitCode, out, err) = capture {
                withFixedDiceRolls("3d6")
            }

            exitCode shouldBe 0
            out shouldBeAfterTrimming """
3d6 10
            """
            err.shouldBeEmpty()
        }

        @Test
        fun `should roll dice from command line in color`() {
            val (exitCode, out, err) = capture {
                // Force color with option parameter
                withFixedDiceRolls("--color=always", "3d6")
            }

            exitCode shouldBe 0
            out shouldBeAfterTrimming """
3d6 @|fg_green,bold 10|@
            """.colored
            err.shouldBeEmpty()
        }

        @Disabled("Find out why mock dies during tests")
        @Test
        fun `should roll dice from command line showing only results`() {
            val (exitCode, out, err) = capture {
                withFixedDiceRolls("--result-only", "3d6")
            }

            exitCode shouldBe 0
            out shouldBeAfterTrimming """
---
10
            """.colored
            err.shouldBeEmpty()
        }

        @Test
        fun `should roll dice from command line verbosely and in color`() {
            val (exitCode, out, err) = capture {
                // Force color with fallback 'always' parameter value
                withFixedDiceRolls("-C", "--verbose", "3d6")
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
            val (exitCode, out, err) = capture {
                withFixedDiceRolls("3d6", "3dd")
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
            val (exitCode, out, err) = capture {
                withFixedDiceRolls("3d")
            }

            exitCode shouldBe 1
            out.shouldBeEmpty()
            err shouldBeAfterTrimming """
roll: Incomplete dice expression '3d'
            """
        }

        @Test
        fun `should fail gnuishly with 1-liner for exploding too low`() {
            val (exitCode, out, err) = capture {
                withFixedDiceRolls("d1!")
            }

            exitCode shouldBe 1
            out.shouldBeEmpty()
            err shouldBeAfterTrimming """
roll: Exploding on 1 will never finish in dice expression 'd1!'
            """
        }

        @Test
        fun `should fail in color`() {
            val (exitCode, out, err) = capture {
                withFixedDiceRolls("--color=always", "3d6", "3d")
            }

            exitCode shouldBe 1
            out shouldBeAfterTrimming """
3d6 @|fg_green,bold 10|@
            """.colored
            // NB -- order of fg_red,bold and bold,fg_red matters
            err shouldBeAfterTrimming """
@|fg_red,bold roll: Incomplete dice expression '3d'|@
            """.colored
        }

        @Test
        fun `should fail debuggingly with stack trace`() {
            val (exitCode, out, err) = capture {
                withFixedDiceRolls("--debug", "3d6", "3d")
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
            err.shouldStartWith(
                """
hm.binkley.dice.rolling.BadExpressionException: Incomplete dice expression '3d'
	at hm.binkley.dice.MainReporter.display(MainReporter.kt:23)
                """.trimIndent()
            )
        }
    }

    @Nested
    inner class Stdin {
        @Test
        fun `should roll dice from STDIN`() {
            val (exitCode, out, err) = captureWithInput(
                "3d6"
            ) { withFixedDiceRolls() }

            exitCode shouldBe 0
            out shouldBeAfterTrimming """
3d6 10
            """
            err.shouldBeEmpty()
        }

        @Test
        fun `should do nothing if STDIN is empty`() {
            val (exitCode, out, err) = captureWithInput {
                withFixedDiceRolls()
            }

            exitCode shouldBe 0
            out.shouldBeEmpty()
            err.shouldBeEmpty()
        }

        @Test
        fun `should do nothing if STDIN is just a blank line`() {
            val (exitCode, out, err) = captureWithInput(
                ""
            ) { withFixedDiceRolls() }

            exitCode shouldBe 0
            out.shouldBeEmpty()
            err.shouldBeEmpty()
        }

        @Test
        fun `should fail gnuishly for STDIN`() {
            val (exitCode, out, err) = captureWithInput(
                "3d6",
                "3d",
            ) { withFixedDiceRolls() }

            exitCode shouldBe 1
            out shouldBeAfterTrimming """
3d6 10
            """
            err shouldBeAfterTrimming """
roll: Incomplete dice expression '3d'
            """
        }
    }

    @Nested
    inner class PlainRepl {
        private val options = Options()
        private val commandLine = picocli.CommandLine(options)
        private val parseResult = ParseResult.builder(CommandSpec.create())
            .build()

        @Test
        fun `should exit on interrupt in REPL the same as shells`() {
            val exitCode = options.exceptionHandler()
                .handleExecutionException(
                    UserInterruptException("I was typing somethi^C"),
                    commandLine,
                    parseResult,
                )

            exitCode shouldBe 130
        }

        @Test
        fun `should roll dice from REPL`() {
            val (exitCode, out, err) = captureWithInput(
                "3d6"
            ) { withFixedDiceRolls("--test-repl") }

            exitCode shouldBe 0
            out shouldBeAfterTrimming """
${DIE_PROMPT}3d6 10
$DIE_PROMPT
            """
            err.shouldBeEmpty()
        }

        @Test
        fun `should do nothing if REPL is just a blank line`() {
            val (exitCode, out, err) = captureWithInput(
                ""
            ) {
                withFixedDiceRolls("--test-repl")
            }

            exitCode shouldBe 0
            // NB -- user typing <ENTER> supplies the newline
            out shouldBeAfterTrimming """
$DIE_PROMPT$DIE_PROMPT
            """
            err.shouldBeEmpty()
        }

        @Test
        fun `should roll dice from REPL in color`() {
            val (exitCode, out, err) = captureWithInput(
                "3d6"
            ) { withFixedDiceRolls("--test-repl", "--color=always") }

            exitCode shouldBe 0
            out shouldBeAfterTrimming """
${DIE_PROMPT}3d6 @|fg_green,bold 10|@
$DIE_PROMPT
            """.colored
            err.shouldBeEmpty()
        }

        @Test
        fun `should change prompt for REPL`() {
            val (exitCode, out, err) = captureWithInput(
                "3d6"
            ) { withFixedDiceRolls("--test-repl", "--prompt", ">") }

            exitCode shouldBe 0
            out shouldBeAfterTrimming """
>3d6 10
>
            """
            err.shouldBeEmpty()
        }

        @Test
        fun `should fail for REPL`() {
            val (exitCode, out, err) = captureWithInput(
                "3d6",
                "3d",
            ) { withFixedDiceRolls("--test-repl") }

            exitCode shouldBe 0
            // NB -- user typing <ENTER> supplies the newline
            out shouldBeAfterTrimming """
${DIE_PROMPT}3d6 10
$DIE_PROMPT$DIE_PROMPT
            """
            err shouldBeAfterTrimming """
Incomplete dice expression '3d'
            """
        }

        @Test
        fun `should expand history in the REPL`() {
            val (exitCode, out, err) = captureWithInput(
                "3d6",
                "!!",
                "3d6!2",
            ) { withFixedDiceRolls("--test-repl") }

            exitCode shouldBe 0
            out shouldBeAfterTrimming """
${DIE_PROMPT}3d6 10
${DIE_PROMPT}3d6 11
${DIE_PROMPT}3d6!2 70
$DIE_PROMPT
            """
            err.shouldBeEmpty()
        }

        @Test
        fun `should not expand history in the REPL`() {
            val (exitCode, out, err) = captureWithInput(
                "3d6",
                "!!",
                "3d6!2",
            ) { withFixedDiceRolls("--test-repl", "--no-history") }

            exitCode shouldBe 0
            out shouldBeAfterTrimming """
${DIE_PROMPT}3d6 10
$DIE_PROMPT${DIE_PROMPT}3d6!2 76
$DIE_PROMPT
            """
            err shouldBeAfterTrimming """
History disabled because of the --no-history option
            """
        }

        @Test
        fun `should fail for bad history expansion`() {
            val (exitCode, out, err) = captureWithInput(
                "!!",
            ) { withFixedDiceRolls("--test-repl") }

            exitCode shouldBe 0
            // NB -- jline3 clears input and re-prompts
            out shouldBeAfterTrimming """
$DIE_PROMPT$DIE_PROMPT
            """
            err shouldBeAfterTrimming """
!!: event not found
            """
        }

        @Test
        fun `should let framework handle unknown exceptions in REPL`() {
            val ex = NullPointerException()
            val thrown = shouldThrow<NullPointerException> {
                options.exceptionHandler().handleExecutionException(
                    ex,
                    commandLine,
                    parseResult,
                )
            }

            thrown shouldBeSameInstanceAs ex
        }
    }

    @Nested
    inner class FancyRepl {
        @Test
        fun `should do nothing for new REPL with no input`() {
            runWithEofConsole {
                val (exitCode, out, err) = capture {
                    withFixedDiceRolls("--test-repl", "--new-repl")
                }

                exitCode shouldBe 0
                out.shouldBeEmpty()
                err shouldBeAfterTrimming """
WARNING: the new REPL is EXPERIMENTAL
                """
            }
        }

        @Test
        fun `should do nothing for new REPL with no input in color`() {
            runWithEofConsole {
                val (exitCode, out, err) = capture {
                    withFixedDiceRolls("--test-repl", "--new-repl", "-C")
                }

                exitCode shouldBe 0
                out.shouldBeEmpty()
                err shouldBeAfterTrimming """
@|red,bold WARNING: the new REPL is EXPERIMENTAL|@
                """.colored
            }
        }
    }

    @Nested
    inner class Demo {
        @Test
        fun `should run demo`() {
            val (exitCode, out, err) = capture {
                withFixedDiceRolls("--demo")
            }

            exitCode shouldBe 0
            out.shouldEndWith("DONE\n")
            err.shouldNotBeEmpty()
        }

        @Test
        fun `should run demo verbosely`() {
            val (exitCode, out, err) = capture {
                withFixedDiceRolls("--demo", "--verbose")
            }

            exitCode shouldBe 0
            out.shouldEndWith("DONE\n")
            err.shouldNotBeEmpty()
        }

        @Test
        fun `should run demo in color`() {
            val (exitCode, out, err) = capture {
                // Force color with option parameter
                withFixedDiceRolls("--demo", "--color=always")
            }

            exitCode shouldBe 0
            out.shouldEndWith("@|bold DONE|@\n".colored)
            err.shouldNotBeEmpty()
        }

        @Test
        fun `should run demo verbosely and in color`() {
            val (exitCode, out, err) = capture {
                // Force color with fallback 'always' parameter value
                withFixedDiceRolls("-C", "--demo", "--verbose")
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
            val (exitCode, out, err) = capture {
                withFixedDiceRolls(
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
            val (exitCode, out, err) = capture {
                withFixedDiceRolls("--verbose", " 1d1 + 1 ")
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
            val (exitCode, out, err) = capture {
                withFixedDiceRolls("1z1-1")
            }

            exitCode shouldBe 0
            out shouldBeAfterTrimming """
1z1-1 -1
            """
            err.shouldBeEmpty()
        }

        @Test
        fun `should fail below 0`() {
            val (exitCode, out, err) = capture {
                withFixedDiceRolls("--minimum=0", "1z1-1")
            }

            exitCode shouldBe 1
            out.shouldBeEmpty()
            err shouldBeAfterTrimming """
roll: Result -1 is below the minimum result of 0
            """
        }
    }
}

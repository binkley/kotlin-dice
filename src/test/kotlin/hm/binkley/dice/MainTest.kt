package hm.binkley.dice

import com.github.stefanbirkner.systemlambda.SystemLambda.catchSystemExit
import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErrNormalized
import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOutNormalized
import com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable
import com.github.stefanbirkner.systemlambda.SystemLambda.withTextFromSystemIn
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import org.parboiled.Parboiled
import org.parboiled.parserunners.ReportingParseRunner

/**
 * Notes:
 * - Nested assertions are needed because `main` calls `System.exit`:
 * assertions on `System.out` and `System.err` must come before the call to
 * `System.exit`.  Be careful to trap exit before capturing output streams;
 * otherwise the exit bubbles up, and does not run the stream assertions
 *
 * - Do not forget line breaks for stream assertion expected values; they
 * are normalized to use `\n` regardless of platform
 *
 * - The random seed is fixed at "123" so tests are reproducible
 */
internal class MainTest {
    @Test
    fun `should not throw exception on roll`() {
        roll("3d6")
    }

    @Test
    fun `should default construct with the RNG`() {
        ReportingParseRunner<Int>(
            Parboiled.createParser(DiceParser::class.java).diceExpression()
        ).run("3d6")
    }

    @Test
    fun `should be helpful()`() {
        val err = tapSystemErrNormalized {
            val out = tapSystemOutNormalized {
                val exitCode = catchSystemExit {
                    main("--help")
                }
                exitCode shouldBe 0
            }
            out.shouldNotBeEmpty()
        }
        err.shouldBeEmpty()
    }

    @Test
    fun `should roll dice from command line()`() {
        val err = tapSystemErrNormalized {
            val out = tapSystemOutNormalized {
                val exitCode = catchSystemExit {
                    main("3d6")
                }
                exitCode shouldBe 0
            }
            out shouldBe "3d6 12\n"
        }
        err.shouldBeEmpty()
    }

    @Test
    fun `should fail if command line is bad`() {
        val err = tapSystemErrNormalized {
            val out = tapSystemOutNormalized {
                val exitCode = catchSystemExit {
                    main("3d6", "x")
                }
                exitCode shouldBe 1
            }
            out shouldBe "3d6 12\n"
        }
        err shouldBe """
Invalid input 'x', expected diceExpression (line 1, pos 1):
x
^


""".trimIndent()
    }

    @Test
    fun `should roll dice from STDIN`() {
        // TODO: This is ugly needing to hack the environment for testing :(
        withEnvironmentVariable("TERM", "dumb").execute {
            withTextFromSystemIn("3d6").execute {
                val err = tapSystemErrNormalized {
                    val out = tapSystemOutNormalized {
                        val exitCode = catchSystemExit {
                            main()
                        }
                        exitCode shouldBe 0
                    }
                    out shouldBe "3d6 12\n"
                }
                err.shouldBeEmpty()
            }
        }
    }

    @Test
    fun `should do nothing if STDIN is empty`() {
        // TODO: This is ugly needing to hack the environment for testing :(
        withEnvironmentVariable("TERM", "dumb").execute {
            withTextFromSystemIn().execute {
                val err = tapSystemErrNormalized {
                    val out = tapSystemOutNormalized {
                        val exitCode = catchSystemExit {
                            main()
                        }
                        exitCode shouldBe 0
                    }
                    out.shouldBeEmpty()
                }
                err.shouldBeEmpty()
            }
        }
    }

    @Test
    fun `should do nothing if STDIN is just a blank line`() {
        // TODO: This is ugly needing to hack the environment for testing :(
        withEnvironmentVariable("TERM", "dumb").execute {
            withTextFromSystemIn("").execute {
                val err = tapSystemErrNormalized {
                    val out = tapSystemOutNormalized {
                        val exitCode = catchSystemExit {
                            main()
                        }
                        exitCode shouldBe 0
                    }
                    out.shouldBeEmpty()
                }
                err.shouldBeEmpty()
            }
        }
    }

    @Test
    fun `should fail if STDIN is bad`() {
        // TODO: This is ugly needing to hack the environment for testing :(
        withEnvironmentVariable("TERM", "dumb").execute {
            withTextFromSystemIn("3d6", "x").execute {
                val err = tapSystemErrNormalized {
                    val out = tapSystemOutNormalized {
                        val exitCode = catchSystemExit {
                            main()
                        }
                        exitCode shouldBe 1
                    }
                    out shouldBe "3d6 12\n"
                }
                err shouldBe """
Invalid input 'x', expected diceExpression (line 1, pos 1):
x
^


""".trimIndent()
            }
        }
    }

    @Test
    fun `seed is optional()`() {
        val err = tapSystemErrNormalized {
            val out = tapSystemOutNormalized {
                val exitCode = catchSystemExit {
                    main(arrayOf("3d6")) // Call real main, not wrapper
                }
                exitCode shouldBe 0
            }
            out.shouldNotBeEmpty()
        }
        err.shouldBeEmpty()
    }

    @Test
    fun `should run demo noisily()`() {
        val err = tapSystemErrNormalized {
            val out = tapSystemOutNormalized {
                val exitCode = catchSystemExit {
                    main("--demo", "--verbose")
                }
                exitCode shouldBe 0
            }
            out.shouldNotBeEmpty()
        }
        err.shouldNotBeEmpty()
    }
}

private fun main(vararg cmdLine: String) = main(
    arrayOf(
        "--seed=123", // Hard-coded for test reproducibility
        *cmdLine,
    )
)

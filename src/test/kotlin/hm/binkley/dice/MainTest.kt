package hm.binkley.dice

import com.github.stefanbirkner.systemlambda.SystemLambda.catchSystemExit
import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErrNormalized
import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOutNormalized
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
    fun `should run demo()`() {
        val err = tapSystemErrNormalized {
            val out = tapSystemOutNormalized {
                val exitCode = catchSystemExit {
                    main("--demo")
                }
                exitCode shouldBe 0
            }
            out.shouldNotBeEmpty()
        }
        err.shouldNotBeEmpty()
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
    fun `should be noisy()`() {
        val err = tapSystemErrNormalized {
            val out = tapSystemOutNormalized {
                val exitCode = catchSystemExit {
                    main("--verbose", "3d6")
                }
                exitCode shouldBe 0
            }
            out.shouldNotBeEmpty()
        }
        err.shouldBeEmpty()
    }

    @Test
    fun `should prompt for dice expressions()`() {
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
}

private fun main(vararg cmdLine: String) = main(
    arrayOf(
        "--seed=123", // Hard-coded for test reproducibility
        *cmdLine,
    )
)

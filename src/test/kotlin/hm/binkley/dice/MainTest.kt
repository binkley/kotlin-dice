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

/**
 * **NB** &mdash; Nested system-lambda handling is needed as `main` calls
 * `System.exit`, hence assertions on `System.out` and `System.err` must come
 * _before_ trapping `System.exit`.; otherwise the exit bubbles out, and the
 * stream assertions do not run
 */
internal class MainTest {
    @Test
    fun `should show basic help`() {
        val err = tapSystemErrNormalized {
            val out = tapSystemOutNormalized {
                val exitCode = catchSystemExit {
                    runMain("--help")
                }
                exitCode shouldBe 0
            }
            out.shouldNotBeEmpty()
        }
        err.shouldBeEmpty()
    }

    @Test
    fun `should show software version`() {
        val err = tapSystemErrNormalized {
            val out = tapSystemOutNormalized {
                val exitCode = catchSystemExit {
                    runMain("--version")
                }
                exitCode shouldBe 0
            }
            out.shouldNotBeEmpty()
        }
        err.shouldBeEmpty()
    }

    @Test
    fun `should roll dice with a default RNG`() {
        val err = tapSystemErrNormalized {
            val out = tapSystemOutNormalized {
                val exitCode = catchSystemExit {
                    main(arrayOf("3d6")) // No seed
                }
                exitCode shouldBe 0
            }
            out.shouldNotBeEmpty()
        }
        err.shouldBeEmpty()
    }

    @Test
    fun `should roll dice from command line`() {
        val err = tapSystemErrNormalized {
            val out = tapSystemOutNormalized {
                val exitCode = catchSystemExit {
                    runMain("3d6")
                }
                exitCode shouldBe 0
            }
            out shouldBeIgnoringLineEndings "3d6 10"
        }
        err.shouldBeEmpty()
    }

    @Test
    fun `should roll dice from command line in color`() {
        val err = tapSystemErrNormalized {
            val out = tapSystemOutNormalized {
                val exitCode = catchSystemExit {
                    runMain("--color", "3d6")
                }
                exitCode shouldBe 0
            }
            out shouldBeIgnoringLineEndings "3d6 10"
        }
        err.shouldBeEmpty()
    }

    @Test
    fun `should roll dice from command line verbosely and in color`() {
        val err = tapSystemErrNormalized {
            val out = tapSystemOutNormalized {
                val exitCode = catchSystemExit {
                    runMain("--verbose", "--color", "3d6")
                }
                exitCode shouldBe 0
            }
            out shouldBeIgnoringLineEndings """
roll(d6) -> 4
roll(d6) -> 1
roll(d6) -> 5
RESULT -> 10
"""
        }
        err.shouldBeEmpty()
    }

    @Test
    fun `should fail if command line is bad`() {
        val err = tapSystemErrNormalized {
            val out = tapSystemOutNormalized {
                val exitCode = catchSystemExit {
                    runMain("3d6", "x")
                }
                exitCode shouldBe 1
            }
            out shouldBeIgnoringLineEndings "3d6 10"
        }
        err shouldBeIgnoringLineEndings """
Invalid input 'x', expected diceExpression (line 1, pos 1):
x
^
"""
    }

    @Test
    fun `should roll dice from STDIN`() {
        // TODO: This is ugly needing to hack the environment for testing :(
        withEnvironmentVariable("TERM", "dumb").execute {
            withTextFromSystemIn("3d6").execute {
                val err = tapSystemErrNormalized {
                    val out = tapSystemOutNormalized {
                        val exitCode = catchSystemExit {
                            runMain()
                        }
                        exitCode shouldBe 0
                    }
                    out shouldBeIgnoringLineEndings "3d6 10"
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
                            runMain()
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
                            runMain()
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
                            runMain()
                        }
                        exitCode shouldBe 1
                    }
                    out shouldBeIgnoringLineEndings "3d6 10"
                }

                err shouldBeIgnoringLineEndings """
Invalid input 'x', expected diceExpression (line 1, pos 1):
x
^
"""
            }
        }
    }

    @Test
    fun `seed is optional`() {
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
    fun `should run demo`() {
        val err = tapSystemErrNormalized {
            val out = tapSystemOutNormalized {
                val exitCode = catchSystemExit {
                    runMain("--demo")
                }
                exitCode shouldBe 0
            }
            out.shouldNotBeEmpty()
        }
        err.shouldNotBeEmpty()
    }

    @Test
    fun `should run demo verbosely`() {
        val err = tapSystemErrNormalized {
            val out = tapSystemOutNormalized {
                val exitCode = catchSystemExit {
                    runMain("--demo", "--verbose")
                }
                exitCode shouldBe 0
            }
            out.shouldNotBeEmpty()
        }
        err.shouldNotBeEmpty()
    }

    @Test
    fun `should run demo in color`() {
        val err = tapSystemErrNormalized {
            val out = tapSystemOutNormalized {
                val exitCode = catchSystemExit {
                    runMain("--demo", "--color")
                }
                exitCode shouldBe 0
            }
            out.shouldNotBeEmpty()
        }
        err.shouldNotBeEmpty()
    }

    @Test
    fun `should run demo verbosely and in color`() {
        val err = tapSystemErrNormalized {
            val out = tapSystemOutNormalized {
                val exitCode = catchSystemExit {
                    runMain("--demo", "--color", "--verbose")
                }
                exitCode shouldBe 0
            }
            out.shouldNotBeEmpty()
        }
        err.shouldNotBeEmpty()
    }
}

private fun runMain(vararg cmdLine: String) = main(
    arrayOf(
        "--seed=${TESTING_SEED}", // Hard-coded for reproducibility
        *cmdLine,
    )
)

/** @todo Kotlin portable equivalent of `strip()` */
private infix fun String.shouldBeIgnoringLineEndings(expected: String) =
    trimIndent().strip() shouldBe expected.trimIndent().strip()

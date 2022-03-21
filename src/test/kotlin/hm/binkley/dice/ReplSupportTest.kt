package hm.binkley.dice

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.jupiter.api.Test
import java.lang.System.console

internal class ReplSupportTest {
    @Test
    fun `should GNU prefix`() {
        null.maybeGnuPrefix() shouldBe "roll: "
        "Cool".maybeGnuPrefix() shouldBe "roll: Cool"

        mockkStatic(System::class) {
            val eofConsole = eofConsole() // Do not inline -- confuses mockk
            every { console() } returns eofConsole

            null.maybeGnuPrefix() shouldBe ""
            "Cool".maybeGnuPrefix() shouldBe "Cool"
        }
    }

    @Test
    fun `should create old real REPL`() {
        Options().parseOptions(
        )
    }

    @Test
    fun `should create old real REPL without history`() {
        Options().parseOptions(
            "--no-history"
        )
    }

    @Test
    fun `should create new real REPL`() {
        mockkStatic(System::class) {
            val eofConsole = eofConsole() // Do not inline -- confuses mockk
            every { console() } returns eofConsole

            Options().parseOptions(
                "--new-repl"
            )
        }
    }

    @Test
    fun `should create new real REPL without history`() {
        mockkStatic(System::class) {
            val eofConsole = eofConsole() // Do not inline -- confuses mockk
            every { console() } returns eofConsole

            Options().parseOptions(
                "--no-history", "--new-repl"
            )
        }
    }
}

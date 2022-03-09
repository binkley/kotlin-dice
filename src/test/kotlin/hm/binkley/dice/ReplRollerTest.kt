package hm.binkley.dice

import org.junit.jupiter.api.Test

internal class ReplRollerTest {
    @Test
    fun `should create real REPL`() {
        val options = Options() // all defaults

        newRealRepl(options)
    }

    @Test
    fun `should create real REPL without history`() {
        val options = Options()
        options.history = false

        newRealRepl(options)
    }
}

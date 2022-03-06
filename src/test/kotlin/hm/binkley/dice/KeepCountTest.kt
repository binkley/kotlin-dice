package hm.binkley.dice

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import java.lang.System.identityHashCode

internal class KeepCountTest {
    @Test
    fun `should pretty print`() {
        KeepHigh(1).toString() shouldBe "KeepHigh(1)"
        KeepLow(1).toString() shouldBe "KeepLow(1)"
    }

    @Suppress("ReplaceCallWithBinaryOperator")
    @Test
    fun `should equate`() {
        val keep = KeepHigh(1)
        keep.equals(keep).shouldBeTrue()
        keep.equals(KeepHigh(1)).shouldBeTrue()
        keep.equals(null).shouldBeFalse()
        keep.equals(this).shouldBeFalse()
        keep.equals(KeepLow(1)).shouldBeFalse()
        keep.equals(KeepHigh(2)).shouldBeFalse()
    }

    @Test
    fun `should hash`() {
        val keep = KeepHigh(1)
        val hash = keep.hashCode()
        hash shouldNotBe identityHashCode(keep)
        hash shouldNotBe KeepHigh(1)
        hash shouldNotBe KeepLow(1).hashCode()
    }
}

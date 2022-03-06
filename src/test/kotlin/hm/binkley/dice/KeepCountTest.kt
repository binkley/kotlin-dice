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
        KeepMiddleLow(1).toString() shouldBe "KeepMiddleLow(1)"
        KeepMiddleHigh(1).toString() shouldBe "KeepMiddleHigh(1)"
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

    @Test
    fun `should keep low middle`() {
        val evenList = listOf(0, 1, 2, 3)
        val oddList = listOf(0, 1, 2, 3, 4)

        val zeroKeepMiddle = KeepMiddleLow(0)
        zeroKeepMiddle.partition(evenList) shouldBe
                (listOf<Int>() to listOf(0, 1, 2, 3))
        zeroKeepMiddle.partition(oddList) shouldBe
                (listOf<Int>() to listOf(0, 1, 2, 3, 4))

        val oneKeepMiddle = KeepMiddleLow(1)
        oneKeepMiddle.partition(evenList) shouldBe
                (listOf(1) to listOf(0, 2, 3))
        oneKeepMiddle.partition(oddList) shouldBe
                (listOf(2) to listOf(0, 1, 3, 4))

        val evenKeepMiddle = KeepMiddleLow(2)
        evenKeepMiddle.partition(evenList) shouldBe
                (listOf(1, 2) to listOf(0, 3))
        evenKeepMiddle.partition(oddList) shouldBe
                (listOf(1, 2) to listOf(0, 3, 4))

        val oddKeepMiddle = KeepMiddleLow(3)
        oddKeepMiddle.partition(evenList) shouldBe
                (listOf(0, 1, 2) to listOf(3))
        oddKeepMiddle.partition(oddList) shouldBe
                (listOf(1, 2, 3) to listOf(0, 4))
    }

    @Test
    fun `should keep high middle`() {
        val evenList = listOf(0, 1, 2, 3)
        val oddList = listOf(0, 1, 2, 3, 4)

        val zeroKeepMiddle = KeepMiddleHigh(0)
        zeroKeepMiddle.partition(evenList) shouldBe
                (listOf<Int>() to listOf(0, 1, 2, 3))
        zeroKeepMiddle.partition(oddList) shouldBe
                (listOf<Int>() to listOf(0, 1, 2, 3, 4))

        val oneKeepMiddle = KeepMiddleHigh(1)
        oneKeepMiddle.partition(evenList) shouldBe
                (listOf(2) to listOf(0, 1, 3))
        oneKeepMiddle.partition(oddList) shouldBe
                (listOf(2) to listOf(0, 1, 3, 4))

        val evenKeepMiddle = KeepMiddleHigh(2)
        evenKeepMiddle.partition(evenList) shouldBe
                (listOf(1, 2) to listOf(0, 3))
        evenKeepMiddle.partition(oddList) shouldBe
                (listOf(2, 3) to listOf(0, 1, 4))

        val oddKeepMiddle = KeepMiddleHigh(3)
        oddKeepMiddle.partition(evenList) shouldBe
                (listOf(1, 2, 3) to listOf(0))
        oddKeepMiddle.partition(oddList) shouldBe
                (listOf(1, 2, 3) to listOf(0, 4))
    }
}

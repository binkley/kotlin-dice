package hm.binkley.dice

import java.util.Objects.hash

sealed class KeepCount(protected val value: Int) {
    override fun toString() = "${this::class.simpleName}($value)"

    override fun equals(other: Any?) = this === other ||
            other is KeepCount &&
            javaClass == other.javaClass &&
            value == other.value

    override fun hashCode() = hash(javaClass, value)

    abstract fun partition(other: List<Int>): Pair<List<Int>, List<Int>>
}

class KeepHigh(value: Int) : KeepCount(value) {
    override fun partition(other: List<Int>): Pair<List<Int>, List<Int>> {
        val (kept, dropped) = other.withIndex().partition { (index, _) ->
            other.size - value <= index
        }

        return kept.withoutIndex() to dropped.withoutIndex()
    }
}

class KeepMiddle(value: Int) : KeepCount(value) {
    override fun partition(other: List<Int>): Pair<List<Int>, List<Int>> {
        if (0 == value || other.isEmpty())
            return emptyList<Int>() to other

        // Cases for list & keep value (round down):
        // 1. Even / even
        //    (0, [1*, 2], 3)
        //*   - split -> [0, 1] & [2, 3]
        //      - keep -> 1 & 1
        //      - mid -> lower max
        // 2. Even / odd
        //    ([0, 1*, 2], 3)
        //*   - split -> [0, 1] & [2, 3]
        //      - keep -> 2 & 1
        //      - mid -> lower max
        //    (0, [1*], 2, 3)
        //    - split -> [0, 1] & [2, 3]
        //      - keep -> 1 & 0
        //      - mid -> lower max
        // 3. Odd / even -
        //    (0, [1, 2*], 3, 4)
        //*   - split A -> [0, 1, 2] & [3, 4]
        //      - keep -> 2 & 0
        //      - mid -> lower max
        //    - split B -> [0, 1] & [2, 3, 4]
        //      - keep -> 1 & 1
        //      - mid -> upper min
        // 4. Odd / odd
        //    (0, [1, 2*, 3], 4)
        //*   - split A -> [0, 1, 2] & [3, 4]
        //      - keep -> 2 & 1
        //      - mid -> lower max
        //    - split B -> [0, 1] & [2, 3, 4]
        //      - keep -> 1 & 2
        //      - mid -> upper min
        //    (0, 1, [2*], 3, 4)
        //*   - split A -> [0, 1, 2] & [3, 4]
        //      - keep -> 1 & 0
        //      - mid -> lower max
        //    - split B -> [0, 1] & [2, 3, 4]
        //      - keep -> 0 & 1
        //      - mid -> upper min

        // *INVARIANT* After splitting list with lower getting remainder,
        // - 1 (mid) -- start at max of lower
        // - 2 -- mid, then down 1 in lower
        // - 3 -- min of upper
        // - 4 -- down 1 in lower
        // - 5 -- up 1 in upper

        val listEven = 0 == other.size % 2
        val keepEven = 0 == value % 2

        val lowerSize =
            if (listEven) other.size / 2
            else other.size / 2 + 1
        val lowerKeepHigh =
            if (listEven && keepEven) value / 2
            else value / 2 + 1
        val upperKeepLow = value - lowerKeepHigh

        val (lower, upper) = KeepLow(lowerSize).partition(other)
        val (lowerKept, lowerDropped) =
            KeepHigh(lowerKeepHigh).partition(lower)
        val (upperKept, upperDropped) =
            KeepLow(upperKeepLow).partition(upper)

        return (lowerKept + upperKept) to (lowerDropped + upperDropped)
    }
}

class KeepLow(value: Int) : KeepCount(value) {
    override fun partition(other: List<Int>): Pair<List<Int>, List<Int>> {
        val (kept, dropped) = other.withIndex().partition { (index, _) ->
            value > index
        }

        return kept.withoutIndex() to dropped.withoutIndex()
    }
}

private fun <T> List<IndexedValue<T>>.withoutIndex() = map { (_, it) -> it }

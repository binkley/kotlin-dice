package hm.binkley.dice

import kotlin.random.Random

internal const val TESTING_SEED = 1L

/**
 * Recreate a new `Random` for each test so results are stable and
 * independent of the order tests run; avoid reusing the _same_ `Random`
 * object.
 */
internal fun stableSeedForEachTest() = Random(TESTING_SEED)

internal val silentTestingReporter = RollReporter { }

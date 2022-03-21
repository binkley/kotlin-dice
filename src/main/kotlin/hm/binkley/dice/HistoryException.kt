package hm.binkley.dice

sealed class HistoryException(message: String) : Throwable(message)

/**
 * Extends `Throwable` so that it is neither an `Exception` nor an `Error`.
 * This workaround defeats JLine3 from force-dumping a stack trace.
 */
object HistoryDisabledException :
    HistoryException("History disabled because of the --no-history option")

/**
 * Extends `Throwable` so that it is neither an `Exception` nor an `Error`.
 * This workaround defeats JLine3 from force-dumping a stack trace.
 */
class BadHistoryException(cause: IllegalArgumentException) :
    HistoryException(cause.message!!)

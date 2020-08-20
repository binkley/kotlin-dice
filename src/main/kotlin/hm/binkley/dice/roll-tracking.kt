package hm.binkley.dice

fun interface OnRoll {
    fun onRoll(message: String)
}

object DoNothing : OnRoll {
    // TODO: Change from String to a sealed class with specific details: let
    //       the callback decide how to present the information
    override fun onRoll(message: String) = Unit
}

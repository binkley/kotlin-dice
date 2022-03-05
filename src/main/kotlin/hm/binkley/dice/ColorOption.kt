package hm.binkley.dice

/**
 * Arguments to the `--color` option based on GNU standards.
 * The enum name is identical to the argument and case-sensitive.
 * Example: `--color=always`.
 */
@Suppress("EnumEntryName", "unused")
enum class ColorOption(private val ansi: Boolean?) {
    // Force color
    always(true),
    yes(true),
    force(true),

    // Guess for color
    auto(null),
    tty(null),
    `if-tty`(null),

    // Disable color
    never(false),
    no(false),
    none(false),
    ;

    fun install() {
        when (ansi) {
            null -> System.clearProperty("picocli.ansi")
            else -> System.setProperty("picocli.ansi", "$ansi")
        }
    }
}

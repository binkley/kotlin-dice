package hm.binkley.dice

import hm.binkley.dice.NeedsCommandLine.DoNeedsCommandLine
import hm.binkley.dice.NeedsLineReader.DoNeedsLineReader
import lombok.Generated
import org.jline.reader.History
import org.jline.utils.InfoCmp.Capability.clear_screen
import picocli.CommandLine.Command
import picocli.CommandLine.Model.OptionSpec
import picocli.CommandLine.Parameters
import picocli.CommandLine.ParentCommand
import java.util.Formatter

@Command(
    name = "clear",
    description = ["clear the screen"],
    mixinStandardHelpOptions = true,
)
@Generated
class ClearScreenCommand :
    Runnable,
    NeedsLineReader by DoNeedsLineReader() {

    override fun run() {
        lineReader.terminal.puts(clear_screen)
    }
}

@Command(
    name = "history",
    description = ["list command history excluding this command"],
    mixinStandardHelpOptions = true,
)
@Generated
class HistoryCommand :
    Runnable,
    NeedsCommandLine by DoNeedsCommandLine(),
    NeedsLineReader by DoNeedsLineReader() {
    @Parameters(
        arity = "0..1",
        paramLabel = "N"
    )
    var take: Int = 20

    override fun run() = lineReader.history
        .reversed()
        .take(take + 1) // Do not count the history command itself
        .reversed()
        .forEach { commandLine.out.println(it.format()) }
}

@Command(
    name = "options",
    description = ["view or change options"],
    mixinStandardHelpOptions = true,
)
@Generated
class OptionsCommand :
    Runnable,
    NeedsCommandLine by DoNeedsCommandLine() {
    @ParentCommand
    lateinit var parentCommand: Options

    @Parameters(
        description = [
            "Options to change using long option format with leading '--'",
            "If provided no options, show current option settings"
        ],
        paramLabel = "OPTIONS",
    )
    var arguments: List<String> = emptyList()

    override fun run() {
        if (arguments.isEmpty()) with(parentCommand.commandSpec) {
            options().filterNot {
                it.usageHelp() || it.versionHelp() || it.hidden()
            }.filterNot {
                // Skip "take an action" flags
                it.longestName() in listOf("--copyright", "--demo")
            }.onEach {
// DEBUGGERY
//                if ((parentCommand as Options).debug) it.dump()
            }.map {
                it.format()
            }.forEach {
                commandLine.out.println(it)
            }
        }
        else arguments.map { "--$it" }.toTypedArray().run {
            commandLine.parseArgs(*this)
        }
    }
}

/**
 * Formats history display the same as does JLine3, but fixes the
 * off-by-one bug in JLine3 for displaying executed lines.
 *
 * Note: [History.Entry.index] is 0-based, and shell history expansion is
 * 1-based.
 */
@Generated
private fun History.Entry.format() = "%5d  %s".format(index() + 1, line())

/**
 * Formats the `OptionSpec` to appear as it would on the command line.
 *
 * Sadly, "%*s" for passing the width as a parameter does not work
 * for Java [Formatter.format] as it does for "C" `printf`.
 *
 * @todo This is kinda hacky.  Better would be a method in Picocli
 */
@Generated
private fun OptionSpec.format(): String {
    val value = getValue<Any>()
    val template = when (value) {
        null -> "(%s)"
        is Boolean -> "%s"
        is String -> "%s='%s'"
        else -> "%s=%s"
    }
    return template.format(longestName(), value)
}

// DEBUGGERY

//private fun OptionSpec.dump() {
//    println("---")
//    println("* $this")
//    details.forEach {
//        println(" - ${it.label} -> ${it(this).dump()}")
//    }
//}
//
//private val details = listOf(
//    OptionSpec::arity,
//    OptionSpec::auxiliaryTypes,
//    OptionSpec::defaultValue,
//    OptionSpec::description,
//    OptionSpec::echo,
//    OptionSpec::fallbackValue,
//    OptionSpec::hasInitialValue,
//    OptionSpec::inherited,
//    OptionSpec::initialValue,
//    OptionSpec::interactive,
//    OptionSpec::longestName,
//    OptionSpec::isMultiValue,
//    OptionSpec::names,
//    OptionSpec::negatable,
//    OptionSpec::isOption,
//    OptionSpec::order,
//    OptionSpec::paramLabel,
//    OptionSpec::isPositional,
//    OptionSpec::prompt,
//    OptionSpec::required,
//    OptionSpec::shortestName,
//    OptionSpec::type,
//    OptionSpec::typedValues,
//    OptionSpec::userObject,
//    OptionSpec::getValue,
//)
//
//private val KFunction1<OptionSpec, *>.label: String
//    get() {
//        val label = name.replace("([A-Z])".toRegex(), "-$1").uppercase()
//        return if (returnType == typeOf<Boolean>()) "$label?"
//        else label
//    }
//
//private fun Collection<*>.dump(): String = map { it.dump() }.toString()
//
//private fun Any?.dump() = when (this) {
//    null -> "null"
//    is Array<*> -> asList().dump()
//    is Boolean -> "$this"
//    is Class<*> -> simpleName
//    is Collection<*> -> dump()
//    is KClass<*> -> "$this"
//    is Number -> "$this"
//    is String -> "'$this'"
//    else -> "[${this::class.simpleName}] $this"
//}

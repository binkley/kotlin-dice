<a href="LICENSE.md">
<img src="https://unlicense.org/pd-icon.png" alt="Public Domain" align="right"/>
</a>

# Kotlin Dice Parser

[![build](https://github.com/binkley/kotlin-dice/workflows/build/badge.svg)](https://github.com/binkley/kotlin-dice/actions)
[![issues](https://img.shields.io/github/issues/binkley/kotlin-dice.svg)](https://github.com/binkley/kotlin-dice/issues/)
[![vulnerabilities](https://snyk.io/test/github/binkley/kotlin-dice/badge.svg)](https://snyk.io/test/github/binkley/kotlin-dice)
[![license](https://img.shields.io/badge/license-Public%20Domain-blue.svg)](http://unlicense.org/)

A complete dice expression has these parts:

- 1 or more individual dice expressions, added/subtracted together
- An optional adjustment, added/subtracted at the end

The smallest dice expression is just a die type, eg, `d6` meaning roll a
single, regular 6-sided die.
See [_Dice Expression Syntax_](#dice-expression-syntax) and
[_Examples_](#examples), below, for more interesting expressions.

Try `./roll --demo` for a demonstration, or `./roll --demo --verbose` to
see more in how dice expressions work.
Running `./roll` presents an interactive prompt for entering and evaluating
dice expressions.

## Table of contents

* [Build](#build)
* [Command line](#command-line)
* [Dice expression syntax](#dice-expression-syntax)
* [Examples](#examples)
* [REPL](#repl)
* [API](#api)
* [Code conventions](#code-conventions)
* [TODO](#todo)
* [References](#references)

## Build

**Note** &mdash; CI presently consistently fails owing to troubles with
command line interations for features like tab completion.

* [DependencyCheck](https://github.com/jeremylong/DependencyCheck) scans
  for dependency security issues
* [detekt](https://github.com/arturbosch/detekt) runs static code analysis
  for Kotlin
* [JUnit](https://github.com/junit-team/junit5) runs tests
* [JaCoCo](https://github.com/jacoco/jacoco) measures code coverage
* [ktlint](https://github.com/pinterest/ktlint) keeps code tidy
* [snyk](https://snyk.io/test/github/binkley/kotlin-dice) looks for
  vulnerabilities

Use `./mvnw` (Maven) or `./batect build` (Batect) to build, run tests, and
create a demo program.
Use `./roll` or `./batect demo` to run the demo.

CI uses [Batect](https://batect.dev/) to verify builds and behavior, so
an easy way for you to check your changes before pushing to GitHub.

## Command line

Try `./roll --help` to see this help on the command line:

```
Usage:
roll [-hvV] [--copyright] [--demo] [--no-history] [-C[=WHEN]] [-m=MINIMUM]
[-P=PROMPT] [-s=SEED] [--] [@<filename>...] [EXPRESSION(s)...] [COMMAND]

Description:
Roll dice expressions.

Parameters:
[@<filename>...]     One or more argument files containing options.
[EXPRESSION(s)...]   Dice expressions to roll.

Options:
-C, --color[=WHEN]       Choose color output (always, yes, force, auto, tty,
if-tty, never, no, none).
Default with no option is 'auto'.
Default with option but no WHEN is 'always'.
--copyright          Show the copyright and exit.
--demo               Run the demo and exit.
-h, --help               Show this help message and exit.
-m, --minimum=MINIMUM    Fail roll results below MINIMUM.
Default with no option is no minimum.
--no-history         Do not save history from the REPL.
-P, --prompt=PROMPT      Change the REPL prompt from '🎲 '.
-s, --seed=SEED          Fix RNG seed to SEED for repeatable roll results.
-v, --verbose            Show die rolls as they happens.
-V, --version            Print version information and exit.
--                       This option can be used to separate command-line
options from the list of positional parameters.

Commands:
clear    clear the screen
history  list command history excluding this command
options  view or change options

Input modes:
roll
Run the REPL.
roll <expression(s)>
Show roll results of dice expression(s) and exit.
echo <expression(s)> | roll
Show roll result of dice expression(s) read from STDIN and exit.

Output examples:
roll --seed=1 2d4 2d4 (normal)
2d4 4
2d4 7
roll --seed=1 --verbose 2d4 2d4 (verbose)
---
roll(d4) -> 1
roll(d4) -> 3
2d4 -> 4
---
roll(d4) -> 4
roll(d4) -> 3
2d4 -> 7

Files:
~/.roll_history
This file preserves input history across runs of the REPL.

Error messages:
Incomplete dice expression '<EXPRESSION>'
More characters were expected at the end of EXPRESSION.
Unexpected '<CHAR>' (at position <POS>) in dice expression '<EXPRESSION>'
CHAR was not expected in EXPRESSION at position POS (starting from 1).
Result <ROLL> is below the minimum result of <NUMBER>
ROLL is too low for the NUMBER in the --minimum option.
Exploding on <NUMBER> will never finish in dice expression '<EXPRESSION>'
NUMBER is too low for the number of sides on the die.
History disabled because of the --no-history option
Read a history command ('!' first character) but option set for no history.

Exit codes:
0   Successful completion
1   Bad dice expression
2   Bad program usage
130   REPL interrupted (SIGINT)
```

## Dice expression syntax

Parsing dice expressions turns out to be an interesting programming problem.
This project implements a mashup of several dice expression syntaxes,
drawing inspiration from:

- [_Dice Expressions_](https://wiki.rptools.info/index.php/Dice_Expressions)
- [_Dice notation_](https://en.wikipedia.org/wiki/Dice_notation)
- [_Sophie's
  Dice_](https://sophiehoulden.com/dice/documentation/notation.html)

This project supports these types of expressions:

```
[N]'B'D['r'R]['h'[K]|'m'[K]|'n'[K]|'l'[K]][!|!Z]['x'M|'*'M][+EXP|-EXP...][+A|-A]
```

- N &mdash; number of dice, default roll 1
- B &mdash; either a literal `d` (dice are 1 to D) or `z` (dice are 0 to D-1)
- D &mdash; sides on the die, or `%` for percentile dice (100-sided dice)
- R &mdash; reroll dice this or lower, eg, reroll 1s
- K &mdash; keep dice, discard rest, default keep 1
    - highest rolls (`h`)
    - high middle rolls (`n`)
    - low middle rolls (`m`)
    - lowest rolls (`l`)
- ! &mdash; explode the dice, default explosion is on a max roll
- M &mdash; multiple result
- EXP &mdash; add/subtract more dice expressions
- A &mdash; add/subtract this fixed amount to the result

For example, in _D&amp;D_ a d20 roll with advantage is "2d20h" and with
disadvantage is "2d20l", and in _Star Wars_ an exploding d6 roll is "d6!".

All characters are _case-insensitive_, eg, `d6` and `D6` are the same
expression.

Whitespace is supported **only**:

- At start or end of the complete expression
- Around the `+` and `-` operators between single dice expressions

Notes:

- This is _not_ a general calculator so `1 + 2` does not work
- Picking too low an explosion (1 for 'd' or 0 or 'z' dice) does not work

See [TODO](#todo) for further improvements.

## Examples

- `d6` -- roll 1 6-sided die; "dD" is the minimal possible expression
- `d6x2` -- roll 1 6-sided die, double the result
- `z6` -- roll 1 6-sided die zero-based (0-5); "zD" is the minimal possible
  expression
- `2d%+1` -- roll percentile dice 2 times, sum, and add 1 to the result
- `3d6r1!` -- roll 3 6-sided dice, rerolling 1s, "explode" on 6s
- `3d6r1!5` -- roll 3 6-sided dice, rerolling 1s, "explode" on 5s or 6s
- `2d4+2d6h1` -- roll 2 4-sided dice, sum; roll 2 6-sided dice keeping the
  highest roll; add both results

The [demo examples](./src/main/kotlin/hm/binkley/dice/main.kt) (look at
`demoExpressions`) cover all supported examples.

## REPL

Running [`./roll`](./roll) with arguments or input starts an interactive REPL
(read-evaluate-print loop).
The REPL includes many features, courtesy of
[Picocli](https://picocli.info/) and [JLine3](https://jline.github.io/),
including:

- Rich command-line color and formatting
- Traditional shell key bindings such as `Ctrl-A` or up/down arrow
- Command history (saved to `~/.roll_history`) and expansion (`!!`)

## API

The code falls into two halves:

- Main code for the command-line [`roll`](./roll) shell script
- Library code for the parser and related types

### Main

### Library

The key method is `dice(random, reporter)` in the companion object of
[`DiceParser`](./src/main/kotlin/hm/binkley/dice/rolling/DiceParser.kt).
This creates a reuseable parser and roller.

The `random` parameter is a Kotlin `Random`, and defaults to the system RNG.
The `reporter` parameter is a
[`RollReporter`](./src/main/kotlin/hm/binkley/dice/rolling/RollReporter.kt)
and defaults to "do nothing" (_ie_, no reporting).

The simplest example is:

```kotlin
val dice = dice() // Static import
val result = dice.roll("3d6")
println(result.resultValue)
```

A fancier example might be:

```kotlin
val dice = dice(Random(1)) { rolledDice ->
  with(rolledDice) {
    val die = when (dieBase) {
      ONE -> "d$dieSides"
      ZERO -> "z$dieSides"
    }
    val trace = when (this) {
      is PlainRoll -> "rolled $die was $roll"
      is PlainReroll -> "rerolled $die was $roll"
      is ExplodedRoll -> "exploded $die >= $explodeHigh was $roll"
      is ExplodedReroll -> "exploded reroll $die >= $explodeHigh is $roll"
      is DroppedRoll -> "dropped $die was $roll"
    }
    println(trace)
  }
}
val result = dice.roll("2d20h")
// Above tracing prints here
println("result is ${result.resultValue}")
```

And would output:

```
rolled d20 was 6
rolled d20 was 17
dropped d20 was 6
result is 17
```

## Code conventions

At each top-level part of a dice expression parse (eg, die sides), the parser
saves a local value internally.
By the end of the dice expression, this includes:

- Die sides, ie, number of sides on a die (ex: d4)
- Roll count, or 1 if none specified; ie, number of dice to roll
- Reroll low, or the value of the lowest face on a die if no value is
  provided: rolls of this value or lower are rerolled
- Dice to keep, or "roll count" if none specified; a positive number is
  keep highest, a negative number is keep lowest
- Explosion limit, or "die sides + 1" if none specified
- Adjustment, or 0 when none specified

The parser uses a stack for some cases:

- The final result of the dice expression
- Tracking and applying `+`/`-` sign (add/subtract)
- Applying the adjustment, if any, at the end

## Goals for execution script [`roll`](./roll) and `main()`:

Multiple modes of operation:

- Support-type flags, such as `--help` have colorized output
- Testing -- tests include `main()` as well as supporting code
- REPL -- typical keyboard operations (_eg_, `Ctrl-a`, _et al_)
- Rolls on the command line
- Rolls from STDIN (ie, pipe, etc)
- Use of color and formatting unless requested not to do so
- Simple output (the default) or verbose output as dice roll

Remember to distinguish STDOUT and STDERR, helpful when using `./roll` in
scripts.

## TODO

* Raise exception when exploding from the die base (_eg_ "d6!1") as this will
  never complete
* Support divisors of rolls, ie, a syntax for `2d6/2`
* Support `floor`, `ceil`, etc., to round rolls down/up
* Reroll should support options other than low rolls
* REPL should support syntax like "set verbose on" to toggle cmd line flags

## References

* [roll](https://github.com/matteocorti/roll#examples)
* [_Dice Syntax_](https://rollem.rocks/syntax/)
* [_Dice notation_](https://en.wikipedia.org/wiki/Dice_notation)
* [_Dice Reference_](https://wiki.roll20.net/Dice_Reference)
* [_parboiled_](https://github.com/sirthias/parboiled/wiki) &mdash; the parser

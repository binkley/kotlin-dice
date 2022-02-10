<a href="LICENSE.md">
<img src="https://unlicense.org/pd-icon.png" alt="Public Domain" align="right"/>
</a>

# Kotlin Dice Parser

[![build](https://github.com/binkley/kotlin-dice/workflows/build/badge.svg)](https://github.com/binkley/kotlin-dice/actions)
[![vulnerabilities](https://snyk.io/test/github/binkley/kotlin-dice/badge.svg)](https://snyk.io/test/github/binkley/kotlin-dice)
[![license](https://img.shields.io/badge/license-Public%20Domain-blue.svg)](http://unlicense.org/)

A dice expression has these parts:

- 1 or more roll expressions, added/subtracted together
- An optional adjustment, added/subtracted at the end

The smallest roll expression is just a die type, eg, `d6` meaning roll 1
6-sided die.  See [_Examples_](#examples), below.

Try `./roll --demo` for a demonstration, or `./roll --demo --verbose` to 
see more in how roll expressions work. Running `./roll` prompts presents 
you an interactive prompt for entering and evaluating dice expressions.

## Table of contents

* [Build](#build)
* [Dice expression syntax](#dice-expression-syntax)
* [Examples](#examples)
* [Code conventions](#code-conventions)
* [TODO](#todo)
* [References](#references)

## Build

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
create a demo program.  Use `./roll` or `./batect demo` to run the demo.

[Batect](https://batect.dev/) works "out of the box", however, an important
optimization is to avoid redownloading plugins and dependencies from within
a Docker container.

This project shares Maven plugin and dependency downloads with the Docker 
container run by Batect and CI.

## Dice expression syntax

Parsing dice expressions turned out to be an interesting programming problem.
This project implements a mashup of dice expression syntaxes. Inspirations
drawn from:

- [_Dice Expressions_](https://wiki.rptools.info/index.php/Dice_Expressions)
- [_Dice notation_](https://en.wikipedia.org/wiki/Dice_notation)
- [_Sophie's Dice](https://sophiehoulden.com/dice/documentation/notation.html)

General syntax supported here:

```
[N]'x'D['r'R]['h'K|'l'K][!|!Z][+EXP|-EXP...][+A|-A]
```

- N - number of dice, default 1
- x - either a literal 'd' (1 to D based) or 'z' (0 to D-1 based)
- D - sides on the die, or '%' for percentile dice (same as using 100)
- R - reroll dice this or lower, eg, reroll 1s
- K - keep the highest ('h') or ('l') the lowest rolls
- ! - explode the dice; default explosion is on max die value
- EXP - add/subtract more dice expressions
- A - add/subtract this fixed amount to the result

Single-character prefixes are _case-insensitive_, eg, `d6` and `D6` are the
same roll.

Whitespace in a dice expression is supported **only** around `+` and `-` 
operators.

See [TODO](#todo) for further improvements.

## Examples

- `d6` -- roll 1 6-sided die; "dD" is the minimal possible expression
- `z6` -- roll 1 6-sided die zero-based (0-5); "zD" is the minimal possible 
  expression
- `2d%+1` -- roll percentile dice 2 times, sum, and add 1 to the result
- `3d6r1!` -- roll 3 6-sided dice, rerolling 1s, "explode" on 6s
- `3d6r1!5` -- roll 3 6-sided dice, rerolling 1s, "explode" on 5s or 6s
- `2d4+2d6h1` -- roll 2 4-sided dice, sum; roll 2 6-sided dice keeping the
  highest roll; add both results

## Code conventions

As each top-level part of a roll expression (eg, die type) parse, the parser 
saves a local value internally.  By the end of the roll expression, this
includes:

- Die type, ie, number of die sides
- Roll count, or 1 if none specified; ie, number of dice to roll
- Reroll value, or 0 if none specified; rolls of this value or lower are
  rerolled
- Dice to keep, or "roll count" if none specified; a positive number is
  keep highest, a negative number is keep lowest
- Explosion limit, or "die type + 1" if none specified
- Adjustment, or 0 when none specified

The parser still used a stack for some cases:

- The final result of the dice expression
- Tracking and applying `+`/`-` sign (add/subtract)
- Applying the adjustment, if any, at the end

## Goals for execution script [`roll`](./roll) and `main()`:

Multiple modes of operation:

- Support-type flags, such as `--help` should have colorized output
- Testing -- test main and the modes when possible without hosing streams
- REPL -- full colorizing and keyboard controls (_eg_, `Ctrl-a`, _et al_)
- Rolls on the command line -- no colorizing, just print to STDOUT
- Rolls from STDIN (ie, pipe, etc) -- no colorizing, just print to STDOUT

In these, distinguish STDOUT from STDERR.  Think of scripting use cases.

## TODO

* Error messages for bad input are **cryptic**
* Support factors of rolls, ie, a syntax for `(2d6)*2` or `(2d6)/2`
* Support grammar for `4d6-L` or `4d6-H` meaning drop the lowest or highest
* Support `floor`, `ceil`, etc., to round rolls down/up
* Reroll should support options other than low rolls
* REPL should support syntax like "set verbose on" to toggle cmd line flags

## References

* [roll](https://github.com/matteocorti/roll#examples)
* [_Dice Syntax_](https://rollem.rocks/syntax/)
* [_Dice notation_](https://en.wikipedia.org/wiki/Dice_notation)
* [_Dice Reference_](https://wiki.roll20.net/Dice_Reference)
* [_parboiled_](https://github.com/sirthias/parboiled/wiki) &mdash; the parser

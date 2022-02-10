#!/usr/bin/env bash
# shellcheck disable=SC2214,SC2215

# Edit these to suit
readonly package=hm.binkley.dice
readonly artifactId=kotlin-dice
readonly version=0-SNAPSHOT
jvm_flags=(-ea --add-opens java.base/java.lang=ALL-UNNAMED)
# No editable parts below here

export PS4='+${BASH_SOURCE}:${LINENO}:${FUNCNAME[0]:+${FUNCNAME[0]}():} '

set -e
set -u
set -o pipefail

readonly progname="${0##*/}"

# Check for terminal output vs pipe, etc, for formatting
if [ -t 1 ]; then
    printf -v preset "\e[0m"
    printf -v pbold "\e[1m"
    printf -v pitalic "\e[3m"
    printf -v pyellow "\e[33m"
else
    printf -v preset ""
    printf -v pbold ""
    printf -v pitalic ""
    printf -v pyellow ""
fi

function print-help() {
    cat <<EOH
Usage: $pbold$progname$preset [$pyellow-d$preset] [-- ${pyellow}PROGRAM-ARGUMENTS$preset]
Roll dice.

Options:
  $pyellow--debug$preset      Show run script execution to STDERR.
  $pyellow-h$preset, $pyellow--help$preset   Show this help message and exit.

Examples:
  $pbold$progname$preset
     Start the interactive dice rolling prompt.
  $pbold$progname$preset <${pitalic}expression$preset>
     Print result of dice expresion, and exit.
  echo $pitalic<expression>$preset | $pbold$progname$preset
     Print result of STDIN as a dice expression, and exit.

Exit codes:
  ${pbold}0$preset - Successful completion
  ${pbold}1$preset - Bad dice expression
  ${pbold}2$preset - Bad program usage
EOH
}

function bad-build-tool() {
    local tool="$1"

    cat <<EOM
$progname: invalid build tool -- '$tool'
Try '$progname --help' for more information.
EOM
}

function bad-language() {
    local language="$1"

    cat <<EOM
$progname: invalid language -- '$language'
Try '$progname --help' for more information.
EOM
}

function bad-option() {
    local opt="$1"

    cat <<EOM
$progname: invalid option -- '$opt'
Try '$progname --help' for more information.
EOM
}

function mangle-kotlin-classname() {
    local IFS=.

    local -a parts
    read -r -a parts <<<"$1"
    local last="${parts[-1]}"

    case "$last" in
    *Kt) ;;
    *) last="${last}Kt" ;;
    esac
    last="${last//-/_}"
    last=""${last^}

    parts[-1]="$last"

    echo "${parts[*]}"
}

function runtime-classname() {
    case "$language" in
    java) echo "$package.$alt_class" ;;
    kotlin) mangle-kotlin-classname "$package.$alt_class" ;;
    esac
}

function rebuild-if-needed() {
    # TODO: Rebuild if build script is newer than jar
    [[ -e "$jar" && -z "$(find src/main -type f -newer "$jar")" ]] && return

    ./mvnw --strict-checksums -Dmaven.test.skip=true package
}

alt_class=''
debug=false
while getopts :d:h-: opt; do
    [[ $opt == - ]] && opt=${OPTARG%%=*} OPTARG=${OPTARG#*=}
    case $opt in
    debug) debug=true ;;
    h | help)
        print-help
        exit 0
        ;;
    *)
        bad-option "$opt"
        exit 2
        ;;
    esac
done
shift $((OPTIND - 1))

$debug && set -x

if [[ ! -x "./mvnw" ]]; then
    echo "$progname: Not executable: ./mvnw" >&2
    exit 2
fi
readonly jar=target/$artifactId-$version-jar-with-dependencies.jar

jvm_flags=("${jvm_flags[@]}" -jar "$jar")

rebuild-if-needed

exec java "${jvm_flags[@]}" "$@"

#!/bin/sh

./mvnw "$@" &&
    exec java -jar target/kotlin-dice-0-SNAPSHOT-jar-with-dependencies.jar

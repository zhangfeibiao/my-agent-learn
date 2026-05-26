#!/usr/bin/env sh
set -eu

JAVA_HOME="${AGENT_JAVA_HOME:-/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home}"
JAVAC="$JAVA_HOME/bin/javac"
JAR="$JAVA_HOME/bin/jar"

rm -rf target/classes target/personal-knowledge-agent.jar
mkdir -p target/classes

MAIN_SOURCES="$(find src/main/java -name '*.java' | sort)"
"$JAVAC" --release 17 -encoding UTF-8 -d target/classes $MAIN_SOURCES
"$JAR" --create --file target/personal-knowledge-agent.jar --main-class com.example.agentlearn.cli.Main -C target/classes .

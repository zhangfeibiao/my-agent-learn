#!/usr/bin/env sh
set -eu

JAVA_HOME="${AGENT_JAVA_HOME:-/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home}"
JAVAC="$JAVA_HOME/bin/javac"
JAVA="$JAVA_HOME/bin/java"

rm -rf target/classes target/test-classes
mkdir -p target/classes target/test-classes

MAIN_SOURCES="$(find src/main/java -name '*.java' | sort)"
if [ -n "$MAIN_SOURCES" ]; then
  "$JAVAC" --release 17 -encoding UTF-8 -d target/classes $MAIN_SOURCES
fi

TEST_SOURCES="$(find src/test/java -name '*.java' | sort)"
if [ -n "$TEST_SOURCES" ]; then
  "$JAVAC" --release 17 -encoding UTF-8 -cp target/classes -d target/test-classes $TEST_SOURCES
fi

for class_file in $(find target/test-classes -name '*Test.class' | sort); do
  class_name="$(printf '%s' "$class_file" | sed 's#target/test-classes/##; s#/#.#g; s#\.class$##')"
  printf 'Running %s\n' "$class_name"
  "$JAVA" -ea -cp target/classes:target/test-classes "$class_name"
done

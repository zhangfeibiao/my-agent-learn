#!/usr/bin/env sh
set -eu

JAVA_HOME="${AGENT_JAVA_HOME:-/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home}"
export JAVA_HOME
PATH="$JAVA_HOME/bin:$PATH"
export PATH

exec ./.tools/apache-maven-3.9.9/bin/mvn \
  -s .mvn/settings.xml \
  -Dmaven.repo.local=.m2/repository \
  "$@"

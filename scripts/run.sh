#!/usr/bin/env sh
set -eu

JAVA_HOME="${AGENT_JAVA_HOME:-/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home}"
JAVA="$JAVA_HOME/bin/java"

if [ -f .env ]; then
  set -a
  . ./.env
  set +a
fi

if [ ! -f target/personal-knowledge-agent.jar ]; then
  ./scripts/package.sh
fi

exec "$JAVA" -jar target/personal-knowledge-agent.jar "$@"

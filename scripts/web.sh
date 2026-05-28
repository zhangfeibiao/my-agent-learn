#!/usr/bin/env sh
set -eu

if [ -f .env ]; then
  set -a
  . ./.env
  set +a
fi

JAVA_HOME="${AGENT_JAVA_HOME:-/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home}"
JAVA="$JAVA_HOME/bin/java"
PORT="${AGENT_WEB_PORT:-8080}"

./scripts/package.sh
"$JAVA" -cp target/classes com.example.agentlearn.web.WebMain "$PORT"

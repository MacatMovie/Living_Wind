#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
./gradlew clean build "$@"
printf '\nBuild complete. JAR is in build/libs/\n'

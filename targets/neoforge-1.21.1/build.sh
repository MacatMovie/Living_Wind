#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
gradle --no-daemon clean build
printf '\nBuild complete. JAR is in build/libs/\n'

#!/bin/sh

cd `dirname $0`

rm -r build && \
./gradlew clean && \
./gradlew jmh publishResults

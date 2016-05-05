#!/bin/sh

cd `dirname $0`

git fetch && \
git rebase origin/master && \
rm -r build && \
./gradlew clean && \
./gradlew jmh publishResults

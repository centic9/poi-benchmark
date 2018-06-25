#!/bin/sh

cd `dirname $0`

git fetch && \
git rebase origin/master && \
rm -rf build && \
./gradlew clean && \
./gradlew jmh publishResults processResults | tee benchmark.log && \
git add results && git ci -m "Add daily results" && \
git push

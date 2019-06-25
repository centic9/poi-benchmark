#!/bin/sh

cd `dirname $0`

git fetch > benchmark.log && \
git rebase origin/master >> benchmark.log && \
rm -rf build >> benchmark.log && \
./gradlew clean >> benchmark.log && \
./gradlew --no-parallel --no-daemon jmh >> benchmark.log && \
./gradlew --no-parallel --no-daemon publishResults processResults >> benchmark.log && \
git add results && git ci -m "[ci skip] Add daily results" >> benchmark.log && \
git push >> benchmark.log

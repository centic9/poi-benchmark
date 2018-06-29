#!/bin/sh

cd `dirname $0`

git fetch && \
git rebase origin/master && \
rm -rf build && \
./gradlew clean && \
./gradlew --no-parallel --no-daemon jmh | tee benchmark.log | tee benchmark.log && \
./gradlew --no-parallel --no-daemon publishResults processResults && \
git add results && git ci -m "Add daily results" && \
git push

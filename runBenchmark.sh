#!/bin/sh

cd `dirname $0`

# use a newer Apache Ant if it is available at this location
export ANT_HOME=/opt/apache-ant-1.10.8
export PATH=${ANT_HOME}/bin:${PATH}

export GRADLE_CMD="--console=plain --no-parallel --no-daemon"

git fetch > benchmark.log && \
git rebase origin/master >> benchmark.log && \
rm -rf build >> benchmark.log && \
./gradlew ${GRADLE_CMD} clean >> benchmark.log && \
./gradlew ${GRADLE_CMD} jmh >> benchmark.log && \
./gradlew ${GRADLE_CMD} publishResults processResults >> benchmark.log && \
git add results && git ci -m "[ci skip] Add daily results" >> benchmark.log && \
git push >> benchmark.log

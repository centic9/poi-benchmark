#!/bin/sh

cd `dirname $0`

# limit memory-usage of Gradle process
export GRADLE_OPTS="-Xmx64m -Dorg.gradle.jvmargs=-Xmx64m"

# use a newer Apache Ant if it is available at this location
# make sure to also adjust these values in BaseBenchmark.java
export ANT_HOME=/opt/apache-ant-1.10.9
export PATH=${ANT_HOME}/bin:${PATH}
export ANT_OPTS="-Xmx512m"

export GRADLE_CMD="--console=plain --no-parallel --no-daemon"

echo `which ant`
ant -version

echo `which javac`
javac -version

git fetch > benchmark.log && \
git rebase origin/master >> benchmark.log && \
rm -rf build >> benchmark.log && \
./gradlew ${GRADLE_CMD} clean >> benchmark.log && \
./gradlew ${GRADLE_CMD} jmhJar >> benchmark.log && \
mkdir -p build/reports/jmh/ && \
java -Xmx8m -jar build/libs/poi-benchmark-jmh.jar \
  -o build/reports/jmh/human.txt \
  -rf JSON \
  -rff build/reports/jmh/results.json && \
./gradlew ${GRADLE_CMD} publishResults processResults >> benchmark.log && \
git add results && git ci -m "[ci skip] Add daily results" >> benchmark.log && \
git push >> benchmark.log

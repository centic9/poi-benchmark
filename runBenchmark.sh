#!/bin/sh

cd `dirname $0`

# limit memory-usage of Gradle process
export GRADLE_OPTS="-Xmx64m -Dorg.gradle.jvmargs=-Xmx64m"

export GRADLE_CMD="--console=plain --no-parallel --no-daemon"

echo `which javac` > benchmark.log 2>&1
javac -version >> benchmark.log 2>&1

wget --timestamping https://github.com/bourgesl/marlin-renderer/releases/download/v0_9_4_5_jdk11/marlin-0.9.4.5-Unsafe-OpenJDK11.jar >> benchmark.log 2>&1

wget --timestamping https://github.com/bourgesl/marlin-renderer/releases/download/v0_9_4_5/marlin-0.9.4.5-Unsafe.jar >> benchmark.log 2>&1

git fetch >> benchmark.log 2>&1 && \
git checkout >> results >> benchmark.log 2>&1 && \
git rebase origin/master >> benchmark.log 2>&1 && \
rm -rf build >> benchmark.log 2>&1 && \
\
set >> benchmark.log && \
./gradlew ${GRADLE_CMD} clean >> benchmark.log 2>&1 && \
./gradlew ${GRADLE_CMD} jmhJar >> benchmark.log 2>&1 && \
mkdir -p build/reports/jmh/ && \
java -Xmx8m \
  -jar build/libs/poi-benchmark-jmh.jar \
  -o build/reports/jmh/human.txt \
  -rf JSON \
  -rff build/reports/jmh/results.json >> benchmark.log 2>&1 && \
./gradlew ${GRADLE_CMD} publishResults processResults >> benchmark.log 2>&1
RET=$?
if [ ${RET} -ne 0 ]; then
  echo "Failed to run poi-benchmark"
  exit 1
fi

grep -Ri tenant results >> benchmark.log 2>&1
RET=$?
if [ ${RET} -ne 1 ]; then
  echo "Found invalid text 'tenanttoken' in the result-files, not committing changes"
  exit 2
fi

git add results && git commit -m "[ci skip] Add daily results" >> benchmark.log 2>&1 && \
git push >> benchmark.log 2>&1

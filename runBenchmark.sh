#!/bin/sh

cd `dirname $0`

if [ -f /etc/apache2/sites-available/corpora.conf ];then
  # TikaVM only has AdoptOpenJDK, unfortunately the hostname is not set properly
  # thus we resort to a check on existence of a specific file...
  export JAVA_HOME=/usr/lib/jvm/adoptopenjdk-11-hotspot-amd64
  export PATH=$JAVA_HOME/bin:$PATH
fi

# limit memory-usage of Gradle process
export GRADLE_OPTS="-Xmx80m -Dorg.gradle.jvmargs=-Xmx80m"

export GRADLE_CMD="--console=plain --no-parallel --no-daemon"

echo `which javac` > benchmark.log 2>&1
javac -version >> benchmark.log 2>&1

# https://github.com/bourgesl/marlin-renderer
echo Downloading marlin renderer
wget --timestamping https://github.com/bourgesl/marlin-renderer/releases/download/v0_9_4_8/marlin-0.9.4.8-Unsafe-OpenJDK11.jar >> benchmark.log 2>&1
wget --timestamping https://github.com/bourgesl/marlin-renderer/releases/download/v0_9_4_7_jdk8/marlin-0.9.4.7-Unsafe.jar >> benchmark.log 2>&1

echo Fetching latest from Git >> benchmark.log 2>&1
git fetch >> benchmark.log 2>&1 && \
git checkout results >> benchmark.log 2>&1 && \
git rebase origin/master >> benchmark.log 2>&1 && \
rm -rf build >> benchmark.log 2>&1 && \
\
set | grep -v LS_COLORS | grep -v LESS_TERMCAP >> benchmark.log && \
./gradlew ${GRADLE_CMD} clean >> benchmark.log 2>&1 && \
./gradlew ${GRADLE_CMD} jmhJar >> benchmark.log 2>&1 && \
mkdir -p build/reports/jmh/ && \
java -Xmx16m \
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

echo Summarizing results >> benchmark.log 2>&1
grep -Ri tenant results >> benchmark.log 2>&1
RET=$?
if [ ${RET} -ne 1 ]; then
  echo "Found invalid text 'tenanttoken' in the result-files, not committing changes"
  exit 2
fi

echo Adding results to Git >> benchmark.log 2>&1
git add results && git commit -m "[ci skip] Add daily results" >> benchmark.log 2>&1 && \
git push >> benchmark.log 2>&1

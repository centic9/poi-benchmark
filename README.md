This is a small project which executes various longer running
tests using Apache POI and measures runtimes using
the JMH micro-benchmarking suite.

Things that are executed include

* compiling Apache POI from scratch
* running the various test-suites
* running some of the example applications which
indicate performance of certain areas of functionality

## Cron-entry to run the job every night

0 1 * * * bash /opt/poi-benchmark/runBenchmark.sh

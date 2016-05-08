This is a small project which executes various longer running
tests against Apache POI and measures runtimes using
the JMH micro-benchmarking suite.

Things that are executed include

* compiling Apache POI from scratch
* running the various test-suites
* running some of the example applications which
indicate performance of certain areas of functionality

## Initial URL for looking at results

For now the results can be looked at directly from the Git repository, for github you can use:

https://cdn.rawgit.com/centic9/poi-benchmark/master/results/results.html

However this is likely to change with some better deployment setup in the future.

## Cron-entry to run the job every night

0 1 * * * bash /opt/poi-benchmark/runBenchmark.sh

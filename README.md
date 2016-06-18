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

https://rawgit.com/centic9/poi-benchmark/master/results/results.html

However this is likely to change with some better deployment setup in the future.

## Required software

You currently need the following to be installed
* Java 6 or higher
* Apache Ant 1.8.0 or higher

Make sure to have Apache Ant in the PATH, i.e. typing `ant` on the commandline should work.
If Apache ant is installed via your Linux distribution, also install the package ´ant-optional´ or ´ant-junit´

## Cron-entry to run the job every night

    0 1 * * * bash /opt/poi-benchmark/runBenchmark.sh

## How to setup password-less pushes to github.com

* Go to https://github.com/settings/ssh and follow the steps to create and add a ssh-key
* See http://mattmakesmaps.com/blog/2013/06/16/auto-push-to-github-via-machine-user/ for more details


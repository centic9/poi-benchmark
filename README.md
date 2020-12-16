[![Build Status](https://travis-ci.org/centic9/poi-benchmark.svg)](https://travis-ci.org/centic9/poi-benchmark) [![Gradle Status](https://gradleupdate.appspot.com/centic9/poi-benchmark/status.svg?branch=master)](https://gradleupdate.appspot.com/centic9/poi-benchmark/status)

This is a small project which executes various longer running
tests against Apache POI and measures execution times using
the JMH micro-benchmarking suite.

Things that are executed include

* compiling Apache POI from scratch
* running the various test-suites
* running some of the example applications which
indicate performance of certain areas of functionality

## Current results

The results can be looked at directly from the Git repository, for github you can use:

https://rawgit.com/centic9/poi-benchmark/master/results/results.html

## Required software

You currently need the following to be installed
* Java 8 or higher
* Apache Ant 1.8.0 or higher

Make sure you have Apache Ant available in the PATH, i.e. typing `ant` on the commandline should work.
If Apache ant is installed via your Linux distribution, also install the package ´ant-optional´ or ´ant-junit´

## Cron-entry to run the job every night

    0 1 * * * bash /opt/poi-benchmark/runBenchmark.sh

## How to setup password-less pushes to github.com

* Go to https://github.com/settings/ssh and follow the steps to create and add a ssh-key
* See http://mattmakesmaps.com/blog/2013/06/16/auto-push-to-github-via-machine-user/ for more details

## Email-Setup

There are some configuration values necessary to make sending email work, take a look
at the file `src/main/resources/benchmark.properties.template` and create a file 
`src/main/resources/benchmark.properties` with your values.

Make sure to not commit this file to the VCS!

#### Licensing

   Copyright 2013-2020 Dominik Stadler

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

# SBE Scala Example

## Overview
This is an example of how to use the <a href="https://github.com/real-logic/simple-binary-encoding">Simple Binary Encoding</a> library from Scala.  It was originally based on the SbeExample.java code in that library, and uses the synthesized Java domain classes to perform the encoding and decoding.  I have put those synthesized domain classes into a local JAR for convenience.

This project also uses Konrad Malawski's sbt-jmh plugin to natively run the benchmarks using Aleksey Shipilev and the OpenJDK community's amazing <a href="openjdk.java.net/projects/code-tools/jmh/">Java Microbenchmarking Harness (JMH)</a> tool.

## To Run the JMH benchmark
Type
```
    sbt run
```
at the command line.  You should see the output of the JMH benchmark of the CarBenchmark example encoding/decoding at the console.

## To Run the SbeExample encode/decode
You have to override the default JMH execution provided by <a href="https://github.com/ktoso/sbt-jmh">Konrad Malawski's sbt-jmh plugin</a>.  For now, just use the command line or open in an IDE and execute that class via it's main.

##License
This code is open source software licensed under the <a href="http://www.apache.org/licenses/LICENSE-2.0.html">Apache 2.0 License</a>.

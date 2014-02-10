# SBE Scala Example

## Overview
This is an example of how to use the <a href="https://github.com/real-logic/simple-binary-encoding">Simple Binary Encoding</a> library from Scala.  It is based on the <a href="https://github.com/real-logic/simple-binary-encoding/blob/master/examples/java/uk/co/real_logic/sbe/examples/SbeExample.java">SbeExample.java</a> code in that library, and uses the synthesized Java domain classes to perform the encoding and decoding.  I have put those synthesized domain classes, as well as the SBE library JAR, into local JARs for convenience.  SBE has not been published as a library to an external repository yet, so there's no other way to include them in this project at this time.

## To Run
Type
```
    sbt run
```
at the command line.  You should see the output of the encoding/decoding at the console.

##License
This code is open source software licensed under the <a href="http://www.apache.org/licenses/LICENSE-2.0.html">Apache 2.0 License</a>.

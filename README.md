Sometimes, neatness counts. 
If you are trying to puzzle out a stack trace, 
pick a critical line of text out of a long stream of console output,
or compare two streams of binary data, a little bit of formatting can go a long way.

Thats what _pretty_ is for.  It adds support for pretty output where it counts:

* ANSI font and background color support
* Hex dump of binary data
* Hex dump of binary deltas
* Readable output for exceptions

pretty is released under the terms of the Apache Software License 2.0.

pretty is available from the Clojars artifact repository as `io.aviso:pretty`.
Follow [these instructions](https://clojars.org/io.aviso/pretty) to configure the dependency in your build tool.

## io.aviso.ansi

This namespace defines a number of functions and constants for producing [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code).

```clojure
(println (str "The following text will be " (bold-red "bold and red") "."))
```

For each of the supported colors (black, red, green, yellow, blue, magenta, cyan, and white) there will be four functions and two constants:

* _color_ - function to set text color
* _color_-bg - function to set background color
* bold-_color_ - function to set enable bold text and the text color
* bold-_color_-bg - function to enable bold text and the background color
* _color_-font - constant that enables the text color
* _color_-bg-font - constant that enables the background color

The functions are passed a string and wrap the string with ANSI codes to enable an ANSI graphic representation for the text, with a reset after the text.

In addition there are functions `bold` and `italic` and constants `bold-font`, `italic-font`, and `reset-font`.

The above example could also be written as:

```clojure
(println (str "The following text will be " bold-font red-font "bold and red" reset-font "."))
```

## io.aviso.binary

This namespace support output of binary data.

Binary data is represented using the protocol BinaryData; this protocol is extended on byte arrays, on String, and on nil.
BinaryData is simply a randomly accessible collection of bytes, with a known length.

```clojure
(println (format-binary "Choose immutability and see where it takes you."))
```

```
0000: 43 68 6F 6F 73 65 20 69 6D 6D 75 74 61 62 69 6C 69 74 79 20 61 6E 64 20 73 65 65 20 77 68 65 72
0020: 65 20 69 74 20 74 61 6B 65 73 20 79 6F 75 2E
```

You can also compare two binary data values:

![](https://www.evernote.com/shard/s54/sh/d7d3942b-d99f-4ab7-a572-04186495c49b/841bbc6d91db0a1927a4fbc67336569d/deep/0/REPL%20and%20binary.clj%20-%20%5Bpretty%5D%20-%20pretty%20-%20%5B~/workspaces/annadale/pretty%5D.png)

If the two data are of different lengths, the shorter one is padded with `--` to make up the difference.

## io.aviso.exception

Exceptions in Clojure are extremely painful for many reasons:

* They are often nested (wrapped and rethrown)
* Stack frames reference the JVM class for Clojure functions, leaving the user to demangle the name back to the Clojure name
* Stack traces are output for every exception, which clogs output without providing useful detail
* Stack traces are often truncated, obscuring vital information
* Many stack frames represent implementation details of Clojure that are not relevant

This is addressed by the `format-exception` function, which takes an exception and converts it to a string, ready to be printed to the console.

`format-exception` navigates down the exception hierarchy; it only presents the stack trace for the deepest, or root, exception. It can navigate
any property that returns a non-nil Throwable type, not just the rootCause property; this makes it properly expand older exceptions
that do not set the rootCause property.

It displays the class name of each exception, its message, and any non-nil properties of the exception.

The all-important stack trace is carefully formatted for readability, with the left-most column identifying Clojure functions, the middle columns
presenting the file name and line number, and the right-most columns the Java class and method names.

![](https://www.evernote.com/shard/s54/sh/7df05675-3d07-463e-b27c-195214b2a854/2333cd1a62d550522f6a4534b129dd58/deep/0/REPL%20and%20binary.clj%20-%20%5Bpretty%5D%20-%20pretty%20-%20%5B~/workspaces/annadale/pretty%5D.png)

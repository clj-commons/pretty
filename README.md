[![Clojars](https://img.shields.io/clojars/v/io.aviso/pretty.svg)](http://clojars.org/io.aviso/pretty)
[![CI](https://github.com/AvisoNovate/pretty/actions/workflows/clojure.yml/badge.svg)](https://github.com/AvisoNovate/pretty/actions/workflows/clojure.yml)

*Sometimes, neatness counts*

If you are trying to puzzle out a stack trace, 
pick a critical line of text out of a long stream of console output,
or compare two streams of binary data, a little bit of formatting can go a long way.

That's what `clj-commons/pretty` is for.  It adds support for pretty output where it counts:

* Readable output for exceptions
* ANSI font and background color support
* Hex dump of binary data
* Hex dump of binary deltas
* Formatting data into columns

![Example](docs/images/formatted-exception.png)

Pretty is compatible with Clojure 1.10 and above.

Parts of Pretty can be used with [Babashka](https://book.babashka.org/#introduction), such as the `clj-commons.ansi`
namespace; however, Babashka runs in an interpreter and its approach to exceptions is
incompatible with JVM exceptions.

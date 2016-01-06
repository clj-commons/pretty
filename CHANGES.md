## 0.1.21 - UNRELEASED

Improved docstrings for ANSI font constants and functions.

## 0.1.20 - 4 Dec 2015

Pretty will identify repeating stack frames (for example, from an infinite loop)
and only print the stack frame once, but append the number of times it repeats.

Made an adjustment for Clojure 1.8's new direct linking feature.

Improved the way Pretty acts as a Leiningen plugin.  Pretty exceptions reports are now
produced for both REPL sessions and test executions.

The io.aviso.nrepl namespace has been removed.

[Closed issues](https://github.com/AvisoNovate/pretty/issues?q=milestone%3A0.1.20)

## 0.1.19 - 27 Aug 2015

Print a blank line before the exception output, when reporting a clojure.test exception.
Previously, the first line was was on the same line as the "actual:" label, which 
interfered with columnar output.

The built in stack frame filtering rules are now better documented, and speclj.* is now included as :terminate.

You may now add pretty to your Leiningen :plugins list; it will automatically add the Pretty nREPL
middleware.

[Closed issues](https://github.com/AvisoNovate/pretty/issues?q=milestone%3A0.1.19+is%3Aclosed)


## 0.1.18 - 5 May 2015

io.aviso.repl/install-pretty-logging now installs a default Thread uncaughtExceptionHandler.

There's a new arity of io.aviso.columns/write-rows that streamlines the whole process (it can
calculate column widths automatically).

The Clojure ExceptionInfo exception is now treated specially.

[Closed issues](https://github.com/AvisoNovate/pretty/issues?q=milestone%3A0.1.18+is%3Aclosed)


## 0.1.17 - 18 Feb 2015

Changed io.aviso.logging to always use the current value of *default-logging-filter* rather than capturing
its value when install-pretty-logging is invoked.

Sometimes, the file name of a stack trace element is a complete path (this occurs with some
testing frameworks); in that case, Pretty will now strip off the prefix from the path, when
it matches the current directory path.
This keeps the file name column as narrow as possible.

## 0.1.16 - 4 Feb 2015

io.aviso.exception/*default-frame-filter* has been added, and acts as the default frame filter for
write-exception (previously there was no default).

[Closed issues](https://github.com/AvisoNovate/pretty/issues?q=milestone%3A0.1.16+is%3Aclosed)

## 0.1.15 - 2 Feb 2015

Starting in this release, the exception report layout has changed significantly; however, the old
behavior is still available via the io.aviso.exceptions/*traditional* dynamic var.

A new namespace, io.aviso.logging, includes code to setup clojure.tools.logging to make use of pretty
exception output.

[Closed issues](https://github.com/AvisoNovate/pretty/issues?q=milestone%3A0.1.15+is%3Aclosed)

## 0.1.14 - 9 Jan 2015

[Closed issues](https://github.com/AvisoNovate/pretty/issues?q=milestone%3A0.1.14+is%3Aclosed)

## 0.1.13 - 14 Nov 2014

It is now possible to control how particular types are formatted when printing the
properties of an exception. 
This can be very useful when using (for example) Stuart Sierra's [component](https://github.com/stuartsierra/component)
library.

## 0.1.12 - 27 May 2014

The default stack-frame filter now excludes frames from the `sun.reflect` package (and sub-packages).

For exceptions added via `io.aviso.repl/install-pretty-exceptions`, the filtering omits frames from the `clojure.lang` 
package, and terminates output when the frames for the REPL are reached.

## 0.1.11 - 14 May 2014

It is now possible to specify a _filter_ for stack frames in the exception output.
Frames can be hidden (not displayed at all), or omitted (replaced with '...').

This can remove _significant_ clutter from the exception output, making it that much easier
to identify the true cause of the exception.

[Closed issues](https://github.com/AvisoNovate/pretty/issues?q=milestone%3A0.1.11)




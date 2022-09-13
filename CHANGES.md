## 1.2 -- UNRELEASED

Output from `write-exception` is now buffered; this should reduce the
interleaving of exception output when multiple threads are writing
exceptions simultaneously.

[Closed Issues](https://github.com/AvisoNovate/pretty/issues?q=is%3Aclosed+milestone%3A1.2)

## 1.1.1 -- 15 Dec 20201

Prevent warnings when using with Clojure 1.11.

[Closed Issues](https://github.com/AvisoNovate/pretty/issues?q=is%3Aclosed+milestone%3A1.1.1)

## 1.1 -- 16 May 2021

Restore compatibility with Clojure 1.7.0.

## 1.0 - 16 May 2021

BinaryData protocol extended onto java.nio.ByteBuffer.

[Closed Issues](https://github.com/AvisoNovate/pretty/issues?q=milestone%3A1.0+is%3Aclosed)

## 0.1.37 - 30 Jan 2019

*Incompatible Changes*:

* Removed the `io.aviso.writer` namespace and changed many functions
  to simply write to `*out*` rather than take a writer parameter.

* It is now necessary to setup explicit :middleware in your `project.clj`, as
  Leiningen is phasing out implicit middleware.
  See the manual for more details.

[Closed Issues](https://github.com/AvisoNovate/pretty/issues?q=milestone%3A0.1.37+is%3Aclosed)

## 0.1.36 - 22 Dec 2018

Support Clojure 1.10.

Add support for highlighting application frames using `io.aviso.exception/*app-frame-names*`.

[Closed Issues](https://github.com/AvisoNovate/pretty/issues?q=milestone%3A0.1.36+is%3Aclosed)

## 0.1.35 - 28 Sep 2018

When printing sorted maps, the keys are presented in map order (not sorted).

The new namespace, `io.aviso.component`, can be used to produce concise output
for systems and components that are pretty printed as part of exception output.

[Closed Issues](https://github.com/AvisoNovate/pretty/issues?q=milestone%3A0.1.35+is%3Aclosed)

## 0.1.34 - 28 Jun 2017

Added possibility to disable default ANSI fonts by setting an environment variable `DISABLE_DEFAULT_PRETTY_FONTS`
to any value.

[Closed Issues](https://github.com/AvisoNovate/pretty/issues?q=milestone%3A0.1.34+is%3Aclosed)

## 0.1.33 - 28 Nov 2016

[Closed Issues](https://github.com/AvisoNovate/pretty/issues?q=milestone%3A0.1.33+is%3Aclosed)

## 0.1.32 - 18 Nov 2016

New functions in `io.aviso.repl` for copying text from the clipboard,
and pretty printing it as EDN or as a formatted exception.

## 0.1.31 - 15 Nov 2016

Switch to using macros instead of eval to play nicer with AOT

Support all arities of `clojure.repl/pst`

[Closed Issues](https://github.com/AvisoNovate/pretty/issues?q=milestone%3A0.1.31+is%3Aclosed)

## 0.1.30 - 16 Aug 2016

Fix bad ns declaration and reflection warnings

## 0.1.29 - 19 Jul 2016

Fix an issue where the code injected by the plugin could get damaged by other plugins, resulting in a
ClassNotFoundException.

## 0.1.28 - 15 Jul 2016 

A warning is now produced when using pretty as a plugin, but it is not a 
project dependency.

[Closed Issues](https://github.com/AvisoNovate/pretty/issues?q=milestone%3A0.1.28+is%3Aclosed)

## 0.1.27 - 1 Jul 2016

Qualified keys in exception maps are now printed properly.

## 0.1.26 - 15 Apr 2016

To get around Clojure 1.8 deep linking, `io.aviso.repl/install-pretty-exceptions` now reloads clojure.test
after overriding other functions (such as `clojure.stacktrace/print-stack-trace`).

[Closed Issues](https://github.com/AvisoNovate/pretty/issues?q=milestone%3A0.1.26+is%3Aclosed)

## 0.1.25 - 5 Apr 2016

The writer used in write-exception is now locked and flush on newline is disabled;
this helps ensure that multiple threads do not write their output interspersed
in an unreadable way.

## 0.1.24 - 26 Feb 2016

Internal change to how exception properties are pretty-printed.

## 0.1.23 - 11 Feb 2016

`parse-exception` can now handle method names containing `<` and `>` (used for instance and class
constructor methods), as well as other cases from real-life stack traces.

Stack traces were omitted when the root exception was via `ex-info`; this has been corrected.

[Closed issues](https://github.com/AvisoNovate/pretty/issues?q=milestone%3A0.1.23)

## 0.1.22 - 5 Feb 2016

Fixed a bug where `parse-exception` would fail if the source was "Unknown Source" instead
of a file name and line number.

[Closed issues](https://github.com/AvisoNovate/pretty/issues?q=milestone%3A0.1.22)

## 0.1.21 - 8 Jan 2016

Improved docstrings for ANSI font constants and functions.

Added support for invokePrim() stack frames. 
These are hidden as with Clojure 1.8 invokeStatic() frames.

Stack frames that represent REPL input now appear as `REPL Input` in the file column, rather than
something like `form-init9201216130440431126.clj`.

Source files with extension `.cljc` (introduced in Clojure 1.7) are now recognized as Clojure code.

It is now possible to parse a block of exception text (say, copied from an output log)
so that it may be formatted.
Because of the wide range in which different JDKs may output exceptions, this is considered
experimental.

**Incompatible change:** write-binary now expects an optional map (not a varargs of keys and values)
for options such as :ascii and :line-bytes.

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

Changed io.aviso.logging to always use the current value of \*default-logging-filter\* rather than capturing
its value when install-pretty-logging is invoked.

Sometimes, the file name of a stack trace element is a complete path (this occurs with some
testing frameworks); in that case, Pretty will now strip off the prefix from the path, when
it matches the current directory path.
This keeps the file name column as narrow as possible.

## 0.1.16 - 4 Feb 2015

io.aviso.exception/\*default-frame-filter\* has been added, and acts as the default frame filter for
write-exception (previously there was no default).

[Closed issues](https://github.com/AvisoNovate/pretty/issues?q=milestone%3A0.1.16+is%3Aclosed)

## 0.1.15 - 2 Feb 2015

Starting in this release, the exception report layout has changed significantly; however, the old
behavior is still available via the io.aviso.exceptions/\*traditional\* dynamic var.

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

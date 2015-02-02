## 0.1.16 - UNRELEASED

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

The new documentation site will be available soon.

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




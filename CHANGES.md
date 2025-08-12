## 3.6.3 -- UNRELEASED

- Restore, at least temporarily, the `io.aviso.exception` and `io.aviso.repl shim` namespaces
- Add a shim for `io.aviso.exception/*traditional*`

[Closed Issues](https://github.com/clj-commons/pretty/milestone/62?closed=1)

## 3.6.2 -- 7 Aug 2025

- Fix a bug where the vertical bar for repeated stack frames could be misaligned
- Hide `clojure.lang.RestFn` stack frames

[Closed Issues](https://github.com/clj-commons/pretty/milestone/61?closed=1)

## 3.6.1 -- 4 Aug 2025

Fix a bug where omitted stack frames were printed.

## 3.6.0 -- 4 Aug 2025

*Breaking Changes*

* `clj-commons.ansi`:
  * Support for :pad in `compose` has been removed; use :align
  * The `pcompose` function has been removed; use `pout` instead
* `clj-commons.format.table`
  * Likewise, support for :pad and :title-pad in `print-table` has been removed; use :align and :title-align

*Other Changes*

* `clj-commons.format.table/print-table`
  * The :default-decorator option is now deprecated
  * New column option :formatter will format a column value to a string or composed string
  * New :row-decorator option decorates all columns of a row
  * Column widths may be calculated even from a composed string
* `clj-commons.format.exceptions`
  * Previously, individual stack frames that repeated were identified; Pretty can now identify _sequences_ of repeating
    stack frames
  
[Closed Issues](https://github.com/clj-commons/pretty/milestone/60?closed=1)

## 3.5.0 -- 9 Jul 2025

* Default style for `print-table` can be overridden via a dynamic var
* Use a vertical bar (│) not a pipe character (|) as column separator in binary output
* New `clj-commons.pretty.nrepl` namespace to set up pretty inside nREPL
* New function `clj-commons.pretty.repl/main` meant for use with `clj -X` to wrap another function
* `clj-commons.pretty.annotations`:
  * The :spacing for the default style is now :compact 
  * Can override the style's :marker in a specific annotation
  * Markers must be strings or function (previously chars were allowed)
  * Support for three-character markers added (the middle character is repeated to pad) 
  * Can omit line numbers with `annotate-lines`
* `clj-commons.format.exceceptions/default-frame-rules`:
  * `clojure.core/with-bindings*` and `clojure.core/apply` are now hidden, not omitted
  * terminate at any `clojure.main` function

[Closed Issues](https://github.com/clj-commons/pretty/milestone/59?closed=1)

## 3.4.1 -- 23 Jun 2025

* Removed some reflection warnings
* Changed the default style for tables to use thinner lines

[Closed Issues](https://github.com/clj-commons/pretty/milestone/58?closed=1)

## 3.4.0 -- 16 Jun 2025

* `clj-commons.ansi`:
    * In spans, you may now supply :align with values :left, :right, or :center instead of :pad (:right, :left, :both); support for :pad may be removed in the future
    * Fonts may now include `double-underlined`
    * Fonts may now be `crossed` or `not-crossed` (though this is not universally supported) 
    * Added extended foreground and background colors
    * Added extended foreground and background grey-scale
* `clj-commons.format.table`
    * New `miniminal-style` for table output that uses only spaces to separate columns
    * New :title-align and :align keys for columns to be used instead of :title-pad and :pad (support for which may be removed in the future)
    * Table styles now include a :divider? key which, if true, enables the divider between the title line and the first row of data (previously, the divider was not optional)
* `clj-commons.format.exceptions`
    * `default-frame-rules` was changed to add default rules (to further limit clutter in exceptions reports)
      * omit `clojure.core/apply` and `clojure.core/with-bindings*`
      * omit several functions in `clojure.test`
      
[Closed Issues](https://github.com/clj-commons/pretty/milestone/57?closed=1)

## 3.3.2 - 28 Mar 2025

* Changed some default exception colors to look better against a light background

[Closed Issues](https://github.com/clj-commons/pretty/milestone/56?closed=1)

## 3.3.1 - 23 Jan 2025

Minor bug fixes.

[Closed Issues](https://github.com/clj-commons/pretty/milestone/55?closed=1)

## 3.3.0 - 7 Dec 2024

The new `clj-commons.pretty.annotations` namespace provides functions to help create pretty errors
when parsing or interpretting text:

```text
SELECT DATE, AMT FROM PAYMENTS WHEN AMT > 10000
             ▲▲▲               ▲▲▲▲
             │                 │
             │                 └╴ Unknown token
             │
             └╴ Invalid column name
```

Here, the errors (called "annotations") are presented as callouts targetting specific portions of the input line.

The `callouts` function can handle multiple annotations on a single line, with precise control over styling and layout.

The `annotate-lines` function builds on `callouts` to produce output of multiple lines from some source,
interspersed with callouts:

```text
1: SELECT DATE, AMT
                ▲▲▲              
                │                              
                └╴ Invalid column name
2: FROM PAYMENTS WHEN AMT > 10000
                 ▲▲▲▲                         
                 │               
                 └╴ Unknown token
```                  

The new `clj-commons.pretty.spec` namespace provides type and function specs for the `clj-commons.ansi` and
`clj-commons.pretty.annotations` namespaces.

[Closed Issues](https://github.com/clj-commons/pretty/milestone/54?closed=1)

## 3.2.0 - 20 Sep 2024

Added `clj-commons.ansi/pout` to replace the `pcompose` function; they are identical, but the `pout` name makes more
sense, given that `perr` exists.

Changed how `clj-commons.ansi/compose` creates ANSI SGR strings; this works around an issue in many terminal emulators
where changing boldness from faint to normal, or faint to bold, is not implemented correctly. `compose` now resets fonts
before each font change, which allows such transitions to render correctly.

Added `clj-commons.format.exceptions/default-frame-rules` to supply defaults for `*default-frame-rules*` 
which makes it much easier to override the default rules.

Added function `clj-commons.format.exceptions/format-stack-trace-element` which can be used to convert a Java
StackTraceElement into a demangled, readable string, using the same logic as `format-exception.`

[Closed Issues](https://github.com/clj-commons/pretty/milestone/52?closed=1)

## 3.1.1 - 22 Aug 2024

In a Clojure stack frame, repeated elements may be abbreviated; for example,
what was output in 3.0.0 as
`integration.diplomat.components.github-api-test/fn/fn/fn/fn/fn/fn/fn/fn/fn/fn/fn/fn/fn/fn`
will be output in 3.1.0 as `integration.diplomat.components.github-api-test/fn{x14}`
(this is an actual test case!)
These crazily nested functions occur when using macro-intensive libraries such as
[nubank/state-flow](https://github.com/nubank/state-flow) and [funcool/cats](https://github.com/funcool/cats).

[Closed Issues](https://github.com/clj-commons/pretty/milestone/51?closed=1)

## 3.0.0 - 7 Jun 2024
 
**BREAKING CHANGES**:

Moved the io.aviso/pretty compatibility layer (introduced in 2.5.0) to new library
[org.clj-commons/pretty-aviso-bridge](https://github.com/clj-commons/pretty-aviso-bridge).

Other changes:
* `clj-commons.format.exceptions`
  * Added a cache to speed up transforming Java StackTraceElements
  * Added new functions to make it easier to extract, filter, and format a stack trace outside of formatting an entire exception

[Closed Issues](https://github.com/clj-commons/pretty/milestone/50?closed=1)

## 2.6.0 - 25 Apr 2024

* Font declaration in `compose` can now be a vector of individual terms, rather than a single keyword; e.g. `[:bold :red]` 
  as an alternative to `:bold.red`. This can be useful when the font is computed, rather than a static literal.

[Closed Issues](https://github.com/clj-commons/pretty/milestone/49?closed=1)

## 2.5.1 - 12 Apr 2024

Minor bug fixes.
 
[Closed Issues](https://github.com/clj-commons/pretty/milestone/48?closed=1)

## 2.5.0 - 27 Mar 2024

*BREAKING CHANGES*

* The function `clojure.core/apply` is now omitted (in formatted stack traces)
* Properties inside exceptions are now pretty-printed to a default depth of 2; previously, the depth was unlimited

Other changes:

A limited number of vars and functions defined by the io.aviso/pretty artifact have been added, allowing org.clj-commons/pretty to swap in for io.aviso/pretty in many cases.

[Closed Issues](https://github.com/clj-commons/pretty/milestone/46?closed=1)

## 2.4.0 - 24 Mar 202

*BREAKING CHANGES*

* `clj-commons.format.table/print-table` now centers title columns by default,
  and adds a :title-pad key to the column map to control this explicitly.

Other changes:

`compose` now supports a new value for :pad; the value :both is used to
center the content, adding spaces on both sides.

[Closed Issues](https://github.com/clj-commons/pretty/issues?q=is%3Aclosed+milestone%3A2.4.0)

## 2.3.0 - 9 Mar 2024

A new function, `clj-commons.ansi/perr`, composes its inputs and prints
them to `*err*`, a common behavior for command line tools.

A new namespace, `clj-commons.format.table`, is used to format tabular output; a
prettier version of `clojure.pprint/print-table`

[Closed Issues](https://github.com/clj-commons/pretty/milestone/44?closed=1)

## 2.2.1 - 14 Nov 2023

This release contains only minor bug fixes:

[Closed Issues](https://github.com/clj-commons/pretty/milestone/42?closed=1)

## 2.2 - 1 Sep 2023

This release is bug fixes and minor improvements.  

The new `clj-commons.ansi.pcompose` function is used to compose an ANSI formatted string and then print it,
an exceptionally common case.

The prior restriction with `compose`, that spans nested within spans with a width could not also have a width,
has been removed.

[Closed Issues](https://github.com/clj-commons/pretty/issues?q=is%3Aclosed+milestone%3A2.2)

## 2.1.1 - 18 Aug 2023

Bug fixes

[Closed Issues](https://github.com/clj-commons/pretty/issues?q=is%3Aclosed+milestone%3A2.1.1)

## 2.1 - 11 Aug 2023
 
`install-pretty-exceptions` has been changed to now extend 
`clojure.core/print-method` for Throwable, using `format-exception`.
Exceptions printed by the REPL are now formatted using Pretty; 
further, when using `clojure.test`, when a `thrown-with-msg?` assertion fails, 
the actual exception is now formatted (as this also, indirectly, uses `print-method`).

[Closed Issues](https://github.com/clj-commons/pretty/issues?q=is%3Aclosed+milestone%3A2.1)

## 2.0.2 - 7 Aug 2023

Bug Fixes

[Closed Issues](https://github.com/clj-commons/pretty/issues?q=is%3Aclosed+milestone%3A2.0.2)

## 2.0.1 -- 20 Jul 2023

Bug Fixes

[Closed Issues](https://github.com/clj-commons/pretty/milestone/37?closed=1)

## 2.0 -- 14 Jul 2023

This release moves the library to clj-commons, and changes the root namespace from 
`io.aviso` to `clj-commons`. It strips down the library to its essentials, removing
the `columns`, `component`, and `logging` namespaces entirely.

* Stripped out a lot of redundant documentation
* Reworked the `ansi` namespace to primarily expose the `compose` function and not dozens of constants and functions
* `ansi` determines whether to enable or disable ANSI codes at execution time
* `ansi` now honors the `NO_COLOR` environment variable
* Stripped out code for accessing the clipboard from the `repl` namespace
* Some refactoring inside `exceptions` namespace, including changes to the `*fonts*` var
* Removed the `logging` namespace and dependency on `org.clojure/tools.logging`
* Removed the `component` namespace, but the example is still present in the documentation
* Ensure compatible with Clojure 1.10 and above (now tested in GitHub action)
* The "use -XX:-OmitStackTraceInFastThrow" warning is now formatted, and is output only once
* `write-exception` was renamed to `print-exception`
* `write-binary` and `write-binary-delta` renamed to `print-binary` and `print-binary-delta`
* `compose` can now pad a span of text with spaces (on the left or right) to a desired width
*  Binary output now includes color coding

## 1.4.4 -- 20 Jun 2023

* Fixed: Incorrectly named font terms with `compose`
* Fixed: Incorrect ANSI codes for bright and bright background colors

[Closed Issues](https://github.com/clj-commons/pretty/milestone/36?closed=1)

## 1.4.3 -- 24 May 2023

The `compose` function would collapse blank strings to empty strings.

[Closed Issues](https://github.com/clj-commons/pretty/milestone/33?closed=1)

## 1.4.1, 1.4.2 -- 5 May 2023

`io.aviso.ansi`: Add support for `faint`, `underlined`, and `not-underlined` text, and improvements
to docstrings.

[Closed issues](https://github.com/clj-commons/pretty/milestone/34?closed=1)

## 1.4 -- 27 Mar 2023

A new function, `io.aviso.ansi/compose` uses a [Hiccup](https://github.com/weavejester/hiccup)-inspired
syntax to make composing text with ANSI fonts (foreground and background colors, inverse, bold, and
italic) easy and concise.

The override to enable or disable ANSI text has been amended: the first check is for
a JVM system property, `io.aviso.ansi.enable`, then if that is not set, the `ENABLE_ANSI_COLORS`
environment variable.

[Closed issues](https://github.com/clj-commons/pretty/milestone/32?closed=1)

## 1.3 -- 20 Oct 2022

The default stack frame filter now terminates at any `speclj.*` namespace.

The `io.aviso.ansi` namespace now determines whether output is connected to a terminal,
and disables fonts and colors if so; this can be overridden with the `ENABLE_ANSI_COLORS`
environment variable.

Added a `-main` function to `io.aviso.repl`; this installs pretty exceptions before delegating
to `clojure.main/main`.  Thus, `clojure -m io.aviso.repl -m org.example.myapp` will ultimately
pass any remaining command line arguments to `org.example.myapp/-main`.

The pretty replacement for `clojure.repl/pst` now writes to `*err*`, not `*out*`.

## 1.2 -- 30 Sep 2022

Output from `write-exception` is now buffered; this should reduce the
interleaving of exception output when multiple threads are writing
exceptions simultaneously.

[Closed Issues](https://github.com/clj-commons/pretty/issues?q=is%3Aclosed+milestone%3A1.2)

## 1.1.1 -- 15 Dec 2021

Prevent warnings when using with Clojure 1.11.

[Closed Issues](https://github.com/clj-commons/pretty/issues?q=is%3Aclosed+milestone%3A1.1.1)

## 1.1 -- 16 May 2021

Restore compatibility with Clojure 1.7.0.

## 1.0 - 16 May 2021

BinaryData protocol extended onto java.nio.ByteBuffer.

[Closed Issues](https://github.com/clj-commons/pretty/issues?q=milestone%3A1.0+is%3Aclosed)

## 0.1.37 - 30 Jan 2019

*Incompatible Changes*:

* Removed the `io.aviso.writer` namespace and changed many functions
  to simply write to `*out*` rather than take a writer parameter.

* It is now necessary to setup explicit :middleware in your `project.clj`, as
  Leiningen is phasing out implicit middleware.
  See the manual for more details.

[Closed Issues](https://github.com/clj-commons/pretty/issues?q=milestone%3A0.1.37+is%3Aclosed)

## 0.1.36 - 22 Dec 2018

Support Clojure 1.10.

Add support for highlighting application frames using `io.aviso.exception/*app-frame-names*`.

[Closed Issues](https://github.com/clj-commons/pretty/issues?q=milestone%3A0.1.36+is%3Aclosed)

## 0.1.35 - 28 Sep 2018

When printing sorted maps, the keys are presented in map order (not sorted).

The new namespace, `io.aviso.component`, can be used to produce concise output
for systems and components that are pretty printed as part of exception output.

[Closed Issues](https://github.com/clj-commons/pretty/issues?q=milestone%3A0.1.35+is%3Aclosed)

## 0.1.34 - 28 Jun 2017

Added possibility to disable default ANSI fonts by setting an environment variable `DISABLE_DEFAULT_PRETTY_FONTS`
to any value.

[Closed Issues](https://github.com/clj-commons/pretty/issues?q=milestone%3A0.1.34+is%3Aclosed)

## 0.1.33 - 28 Nov 2016

[Closed Issues](https://github.com/clj-commons/pretty/issues?q=milestone%3A0.1.33+is%3Aclosed)

## 0.1.32 - 18 Nov 2016

New functions in `io.aviso.repl` for copying text from the clipboard,
and pretty printing it as EDN or as a formatted exception.

## 0.1.31 - 15 Nov 2016

Switch to using macros instead of eval to play nicer with AOT

Support all arities of `clojure.repl/pst`

[Closed Issues](https://github.com/clj-commons/pretty/issues?q=milestone%3A0.1.31+is%3Aclosed)

## 0.1.30 - 16 Aug 2016

Fix bad ns declaration and reflection warnings

## 0.1.29 - 19 Jul 2016

Fix an issue where the code injected by the plugin could get damaged by other plugins, resulting in a
ClassNotFoundException.

## 0.1.28 - 15 Jul 2016 

A warning is now produced when using pretty as a plugin, but it is not a 
project dependency.

[Closed Issues](https://github.com/clj-commons/pretty/issues?q=milestone%3A0.1.28+is%3Aclosed)

## 0.1.27 - 1 Jul 2016

Qualified keys in exception maps are now printed properly.

## 0.1.26 - 15 Apr 2016

To get around Clojure 1.8 deep linking, `io.aviso.repl/install-pretty-exceptions` now reloads clojure.test
after overriding other functions (such as `clojure.stacktrace/print-stack-trace`).

[Closed Issues](https://github.com/clj-commons/pretty/issues?q=milestone%3A0.1.26+is%3Aclosed)

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

[Closed issues](https://github.com/clj-commons/pretty/issues?q=milestone%3A0.1.23)

## 0.1.22 - 5 Feb 2016

Fixed a bug where `parse-exception` would fail if the source was "Unknown Source" instead
of a file name and line number.

[Closed issues](https://github.com/clj-commons/pretty/issues?q=milestone%3A0.1.22)

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

[Closed issues](https://github.com/clj-commons/pretty/issues?q=milestone%3A0.1.20)

## 0.1.19 - 27 Aug 2015

Print a blank line before the exception output, when reporting a clojure.test exception.
Previously, the first line was was on the same line as the "actual:" label, which 
interfered with columnar output.

The built in stack frame filtering rules are now better documented, and speclj.* is now included as :terminate.

You may now add pretty to your Leiningen :plugins list; it will automatically add the Pretty nREPL
middleware.

[Closed issues](https://github.com/clj-commons/pretty/issues?q=milestone%3A0.1.19+is%3Aclosed)


## 0.1.18 - 5 May 2015

io.aviso.repl/install-pretty-logging now installs a default Thread uncaughtExceptionHandler.

There's a new arity of io.aviso.columns/write-rows that streamlines the whole process (it can
calculate column widths automatically).

The Clojure ExceptionInfo exception is now treated specially.

[Closed issues](https://github.com/clj-commons/pretty/issues?q=milestone%3A0.1.18+is%3Aclosed)


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

[Closed issues](https://github.com/clj-commons/pretty/issues?q=milestone%3A0.1.16+is%3Aclosed)

## 0.1.15 - 2 Feb 2015

Starting in this release, the exception report layout has changed significantly; however, the old
behavior is still available via the io.aviso.exceptions/\*traditional\* dynamic var.

A new namespace, io.aviso.logging, includes code to setup clojure.tools.logging to make use of pretty
exception output.

[Closed issues](https://github.com/clj-commons/pretty/issues?q=milestone%3A0.1.15+is%3Aclosed)

## 0.1.14 - 9 Jan 2015

[Closed issues](https://github.com/clj-commons/pretty/issues?q=milestone%3A0.1.14+is%3Aclosed)

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

[Closed issues](https://github.com/clj-commons/pretty/issues?q=milestone%3A0.1.11)

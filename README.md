Sometimes, neatness counts. 
If you are trying to puzzle out a stack trace, 
pick a critical line of text out of a long stream of console output,
or compare two streams of binary data, a little bit of formatting can go a long way.

Thats what _pretty_ is for.  It adds support for pretty output where it counts:

* ANSI font and background color support
* Hex dump of binary data
* Hex dump of binary deltas
* Readable output for exceptions
* Formatting data into columns

pretty is released under the terms of the Apache Software License 2.0.

pretty is available from the Clojars artifact repository as `io.aviso:pretty`.
Follow [these instructions](https://clojars.org/io.aviso/pretty) to configure the dependency in your build tool.

[API Documentation](http://howardlewisship.com/io.aviso/pretty/)

## io.aviso.ansi

This namespace defines a number of functions and constants for producing [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code).

```clojure
(println (str "The following text will be " (bold-red "bold and red") "."))
```

![](https://www.evernote.com/shard/s54/sh/117a76f7-9642-41b2-bb4f-35dfc72b9e43/c493c67632b35e2adac8e1f44ee30be6/deep/0/ansi.clj----pretty----pretty------workspaces-annadale-pretty-.png)

For each of the supported colors (black, red, green, yellow, blue, magenta, cyan, and white) there will be four functions and four constants:

* _color_ - function to set text color
* _color_-bg - function to set background color
* bold-_color_ - function to set enable bold text and the text color
* bold-_color_-bg - function to enable bold text and the background color
* _color_-font - constant that enables the text color
* _color_-bg-font - constant that enables the background color
* bold-_color_-font - constant that enables the text color in bold
* bold-_color_-bg-font - constant that enables the background color in bold

The functions are passed a string and wrap the string with ANSI codes to enable an ANSI graphic representation for the text, with a reset after the text.

Note that the exact color interpretation of the ANSI codes varies significantly between platforms and applications, and
is frequently configurable, often using themes. You may need to adjust your application's settings to get an optimum
display.

In addition there are functions `bold`, `inverse`, and `italic` and constants `bold-font`, `inverse-font`, `italic-font`, and `reset-font`.

The above example could also be written as:

```clojure
(println (str "The following text will be " bold-red-font "bold and red" reset-font "."))
```

## io.aviso.binary

This namespace support output of binary data.

Binary data is represented using the protocol BinaryData; this protocol is extended on byte arrays, on String, and on nil.
BinaryData is simply a randomly accessible collection of bytes, with a known length.

```clojure
(write-binary "Choose immutability and see where it takes you.")
```

```
0000: 43 68 6F 6F 73 65 20 69 6D 6D 75 74 61 62 69 6C 69 74 79 20 61 6E 64 20 73 65 65 20 77 68 65 72
0020: 65 20 69 74 20 74 61 6B 65 73 20 79 6F 75 2E
```

`write-binary` can write to a `java.io.Writer` (defaulting to `*out*`) or a `StringBuilder`(or other things, as defined by `io.aviso.writer/Writer` protocol).  The full version explicitly specified where to write to, as well as options:

![](https://www.evernote.com/shard/s54/sh/4211f62b-dec6-4134-be0b-5c7f9261a84f/c488966c5ea16355ce50445401a965e9/deep/0/exception.clj----pretty----pretty------workspaces-annadale-pretty-.png)

Alternately, `format-binary` will return the formatted binary output string.

You can also compare two binary data values with `write-binary-delta`:

![](https://www.evernote.com/shard/s54/sh/dc407aa4-a81e-4851-abed-3ca2949efba1/dfa5d033da855b1a97dd899682ea01fd/deep/0/README.md%20-%20%5Bpretty%5D%20-%20pretty%20-%20%5B~/workspaces/annadale/pretty%5D%20and%20stages.clj%20-%20%5Bswitch%5D%20-%20nexus%20-%20%5B~/workspaces/annadale/nexus%5D.png)

If the two data are of different lengths, the shorter one is padded with `--` to make up the difference.

As with `write-binary`, there's a `format-binary-delta`, and a three-argument version of `write-binary-delta` for specifying a Writer target.

## io.aviso.exception

Exceptions in Clojure are extremely painful for many reasons:

* They are often nested (wrapped and rethrown)
* Stack frames reference the JVM class for Clojure functions, leaving the user to de-mangle the name back to the Clojure name
* Stack traces are output for every exception, which clogs output without providing useful detail
* Stack traces are often truncated, requiring the user to manually re-assemble the stack trace from several pieces
* Many stack frames represent implementation details of Clojure that are not relevant

This is addressed by the `write-exception` function; it take an exception formats it neatly to a Writer, again `*out*` by default.

This is best explained by example; here's a `SQLException` wrapped inside two `RuntimeException`s, and printed normally:

```
(.printStackTrace e)
java.lang.RuntimeException: Request handling exception
	at user$make_exception.invoke(user.clj:30)
	at user$eval1322.invoke(NO_SOURCE_FILE:1)
	at clojure.lang.Compiler.eval(Compiler.java:6619)
	at clojure.lang.Compiler.eval(Compiler.java:6582)
	at clojure.core$eval.invoke(core.clj:2852)
	at clojure.main$repl$read_eval_print__6588$fn__6591.invoke(main.clj:259)
	at clojure.main$repl$read_eval_print__6588.invoke(main.clj:259)
	at clojure.main$repl$fn__6597.invoke(main.clj:277)
	at clojure.main$repl.doInvoke(main.clj:277)
	at clojure.lang.RestFn.invoke(RestFn.java:1096)
	at clojure.tools.nrepl.middleware.interruptible_eval$evaluate$fn__808.invoke(interruptible_eval.clj:56)
	at clojure.lang.AFn.applyToHelper(AFn.java:159)
	at clojure.lang.AFn.applyTo(AFn.java:151)
	at clojure.core$apply.invoke(core.clj:617)
	at clojure.core$with_bindings_STAR_.doInvoke(core.clj:1788)
	at clojure.lang.RestFn.invoke(RestFn.java:425)
	at clojure.tools.nrepl.middleware.interruptible_eval$evaluate.invoke(interruptible_eval.clj:41)
	at clojure.tools.nrepl.middleware.interruptible_eval$interruptible_eval$fn__849$fn__852.invoke(interruptible_eval.clj:171)
	at clojure.core$comp$fn__4154.invoke(core.clj:2330)
	at clojure.tools.nrepl.middleware.interruptible_eval$run_next$fn__842.invoke(interruptible_eval.clj:138)
	at clojure.lang.AFn.run(AFn.java:24)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1110)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:603)
	at java.lang.Thread.run(Thread.java:722)
Caused by: java.lang.RuntimeException: Failure updating row
	at user$update_row.invoke(user.clj:22)
	... 24 more
Caused by: java.sql.SQLException: Database failure
SELECT FOO, BAR, BAZ
FROM GNIP
failed with ABC123
	at user$jdbc_update.invoke(user.clj:6)
	at user$make_jdbc_update_worker$reify__214.do_work(user.clj:17)
	... 25 more
```

... and here's the equivalent, via `write-exception`:

![](https://www.evernote.com/shard/s54/sh/9df8600b-adf2-4605-8298-48d78aa93dd7/e0fccbe84d3de74091ccc0fc3c70d411/deep/0/README.md----pretty----pretty------workspaces-annadale-pretty-.png)

`write-exception` navigates down the exception hierarchy; it only presents the stack trace for the deepest, or root, exception. It can navigate
any property that returns a non-nil Throwable type, not just the rootCause property; this makes it properly expand older exceptions
that do not set the rootCause property.

It displays the class name of each exception, its message, and any non-nil properties of the exception.

The all-important stack trace is carefully formatted for readability, with the left-most column identifying Clojure functions
or Java class and method, and the right columns presenting the file name and line number.

The related function, `format-exception`, produces the same output, but returns it as a string.

For both `format-exception` and `write-exception`, output of the stack trace is optional.

# io.aviso.columns

The columns namespace is what's used by the exceptions namespace to format the exceptions, properties, and stack
traces.

The `format-columns` function is provided with a number of column definitions, each of which describes the width and justification of a column. 
Some column definitions are just a string to be written for that column, such as a column seperator.
`format-columns` returns a function that accepts a StringWriter (such as `*out*`) and the column values.

`write-rows` takes the function provided by `format-columns`, plus a set of functions to extract column values,
plus a seq of rows. In most cases, the rows are maps, and the extraction functions are keywords (isn't Clojure
magical that way?).

Here's an example, from the exception namespace:

```
(defn- write-stack-trace
  [writer exception]
  (let [elements (->> exception expand-stack-trace (map preformat-stack-frame))
        formatter (c/format-columns [:right (c/max-value-length elements :formatted-name)]
                                    "  " (:source *fonts*)
                                    [:right (c/max-value-length elements :file)]
                                    2
                                    [:right (->> elements (map :line) (map str) c/max-length)]
                                    (:reset *fonts*))]
    (c/write-rows writer formatter [:formatted-name
                                    :file
                                    #(if (:line %) ": ")
                                    :line]
                  elements)))
```



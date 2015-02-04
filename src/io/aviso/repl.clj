(ns io.aviso.repl
  "Utilities to assist with REPL-oriented development"
  (:use
  [io.aviso.exception])
  (:require
    [clojure
     [main :as main]
     [repl :as repl]
     [stacktrace :as st]]))

(def ^{:deprecated "0.1.16"}
     standard-frame-filter
  "An alias for [[*default-frame-filter*]], to be removed in an upcoming release."
  *default-frame-filter*)

(defn- reset-var!
  [v override]
  (alter-var-root v (constantly override)))

(defn- write
  [e options]
  (print (format-exception e options))
  (flush))

(defn pretty-repl-caught
  "A replacement for `clojure.main/repl-caught` that prints the exception to `*err*`, without a stack trace or properties."
  [e]
  (write e {:frame-limit 0 :properties false}))

(defn pretty-pst
  "Used as an override of `clojure.repl/pst` but uses pretty formatting. The optional parameter must be an exception
  (it can not be a depth, as with the standard implementation of `pst`)."
  ([] (pretty-pst *e))
  ([e] (write e nil)))

(defn pretty-print-stack-trace
  "Replacement for `clojure.stracktrace/print-stack-trace` and `print-cause-trace`. These functions are used by `clojure.test`."
  ([tr] (pretty-print-stack-trace tr nil))
  ([tr n]
    (write tr {:frame-limit n})))

(defn install-pretty-exceptions
  "Installs an override that outputs pretty exceptions when caught by the main REPL loop. Also, overrides
  `clojure.repl/pst`, `clojure.stacktrace/print-stack-trace`, `clojure.stacktrace/print-cause-trace`.

  Caught exceptions do not print the stack trace; the pst replacement does."
  []
  ;; TODO: Not exactly sure why this works, because clojure.main/repl should be resolving the var to its contained
  ;; function, so the override should not be visible. I'm missing something.
  (reset-var! #'main/repl-caught pretty-repl-caught)
  (reset-var! #'repl/pst pretty-pst)
  (reset-var! #'st/print-stack-trace pretty-print-stack-trace)
  (reset-var! #'st/print-cause-trace pretty-print-stack-trace)
  nil)
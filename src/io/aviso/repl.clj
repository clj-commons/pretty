(ns io.aviso.repl
  "Utilities to assist with REPL-oriented development"
  (:use
    [io.aviso.exception])
  (:require
    [clojure
     [main :as main]
     [repl :as repl]
     [stacktrace :as st]]))

(defn- reset-var!
  [v override]
  (alter-var-root v (constantly override)))

(defn- write
  [e & options]
  (print (apply format-exception e options))
  (flush))

(defn pretty-repl-caught
  "A replacement for clojure.main/repl-caught that prints the exception to *err*, without a stack trace."
  [e]
  (write e :stack-trace false))

(defn pretty-pst
  "Used as an override of clojure.repl/pst but uses pretty formatting. The optional parameter must be an exception
  (it can not be a depth, as with the real pst)."
  ([] (pretty-pst *e))
  ([e] (write e)))

(defn pretty-print-stack-trace
  "Replacement for clojure.stracktrace/print-stack-trace and print-cause-trace."
  ([tr] (pretty-print-stack-trace tr nil))
  ([tr n]
   (write tr :frame-limit n)))

(defn install-pretty-exceptions
  "Installs an override that outputs pretty exceptions when caught by the main REPL loop. Also, overrides
  clojure.repl/pst and clojure.stacktrace/.

  Caught exceptions do not print the stack trace; the pst replacement does."
  []
  ;; TODO: Not exactly sure why this works, because clojure.main/repl should be resolving the var to its contained
  ;; function, so the override should not be visible. I'm missing something.
  (reset-var! #'main/repl-caught pretty-repl-caught)
  (reset-var! #'repl/pst pretty-pst)
  (reset-var! #'st/print-stack-trace pretty-print-stack-trace)
  (reset-var! #'st/print-cause-trace pretty-print-stack-trace)
  nil)
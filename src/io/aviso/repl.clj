(ns io.aviso.repl
  "Utilities to assist with REPL-oriented development"
  (:use
    [io.aviso.exception])
  (:require
    [clojure
     [main :as main]
     [repl :as repl]]))

(defn- reset-var!
  [v override]
  (alter-var-root v (constantly override)))

(defn pretty-repl-caught
  "A replacement for clojure.main/repl-caught that prints the exception to *err*, without a stack trace."
  [e]
  (write-exception *out* e :stack-trace false))

(defn pretty-pst
  "Used as an override of clojure.repl/pst but uses pretty formatting. The optional parameter must be an exception
  (it can not be a depth, as with the real pst)."
  ([] (pretty-pst *e))
  ([e] (write-exception e)))


(defn install-pretty-exceptions
  "Installs an override that outputs pretty exceptions when caught by the main REPL loop. Also, overrides
  clojure.repl/pst.

  Caught exceptions do not print the stack trace; the pst replacement does."
  []
  (reset-var! #'main/repl-caught pretty-repl-caught)
  (reset-var! #'repl/pst pretty-pst)
  nil)
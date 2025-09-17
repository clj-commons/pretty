(ns io.aviso.repl
  "A shim in org.clj-commons/pretty to emulate a few key functions from io.aviso/pretty.

  This namespace may be deleted in a future release."
  {:added "2.5.0"
   :deprecated "2.5.0"}
  (:require [clj-commons.pretty.repl :as repl]))

(defn install-pretty-exceptions
  []
  (repl/install-pretty-exceptions))

(defn uncaught-exception-handler
  []
  (repl/uncaught-exception-handler))

(defn pretty-print-stack-trace
  "Replacement for `clojure.stacktrace/print-stack-trace` and `print-cause-trace`. These functions are used by `clojure.test`."
  ([tr] (repl/pretty-print-stack-trace tr))
  ([tr n]
   (println)
   (repl/pretty-print-stack-trace tr n)))

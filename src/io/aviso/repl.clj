(ns io.aviso.repl
  "Utilities to assist with REPL-oriented development"
  (:use
    [io.aviso.exception])
  (:require
    [clojure
     [main :as main]
     [repl :as repl]
     [stacktrace :as st]]))

(defn standard-frame-filter
  "Default stack frame filter used when printing REPL exceptions. This will omit frames in the `clojure.lang` package,
  and terminates the stack trace at the read-eval-print loop frame. This tends to be very concise; you can use
  `(write-exception *e)` to display the full stack trace."
  [frame]
  (cond
    (-> frame :package (= "clojure.lang"))
    :omit

    (-> frame :name (.startsWith "clojure.main/repl/read-eval-print/"))
    :terminate

    :else
    :show))

(defn- reset-var!
  [v override]
  (alter-var-root v (constantly override)))

(defn- write
  [e options]
  (print (format-exception e (assoc options :filter standard-frame-filter)))
  (flush))

(defn pretty-repl-caught
  "A replacement for clojure.main/repl-caught that prints the exception to *err*, without a stack trace or properties."
  [e]
  (write e {:frame-limit 0 :properties false}))

(defn pretty-pst
  "Used as an override of clojure.repl/pst but uses pretty formatting. The optional parameter must be an exception
  (it can not be a depth, as with the real pst)."
  ([] (pretty-pst *e))
  ([e] (write e nil)))

(defn pretty-print-stack-trace
  "Replacement for clojure.stracktrace/print-stack-trace and print-cause-trace."
  ([tr] (pretty-print-stack-trace tr nil))
  ([tr n]
   (write tr {:frame-limit n})))

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
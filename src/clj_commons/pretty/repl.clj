(ns clj-commons.pretty.repl
  "Utilities to assist with REPL-oriented development."
  (:require [clj-commons.format.exceptions :as e :refer [print-exception]]
            [clojure.main :as main]
            [clojure.repl :as repl]
            [clojure.stacktrace :as st])
  (:import (clojure.lang RT)
           (java.io Writer)))

(defn- reset-var!
  [v override]
  (alter-var-root v (constantly override)))

(defn pretty-repl-caught
  "A replacement for `clojure.main/repl-caught` that prints the exception to `*err*`, without a stack trace or properties."
  [e]
  (binding [*out* *err*]
    (print-exception e {:frame-limit 0 :properties false})))

(defn uncaught-exception-handler
  "Returns a reified UncaughtExceptionHandler that prints the formatted exception to `*err*`."
  {:added "0.1.18"}
  []
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException [_ _ t]
      (binding [*out* *err*]
        (printf "Uncaught exception in thread %s:%n%s%n"
                (-> (Thread/currentThread) .getName)
                (e/format-exception t))
        (flush)))))


(defn pretty-pst
  "Used as an override of `clojure.repl/pst` but uses pretty formatting.  Output is written to `*err*`."
  ([] (pretty-pst *e))
  ([e-or-depth]
   (if (instance? Throwable e-or-depth)
     (pretty-pst e-or-depth nil)
     (pretty-pst *e e-or-depth)))
  ([e depth]
   (binding [*out* *err*]
     (print-exception e (when depth
                          {:frame-limit depth})))))

(defn pretty-print-stack-trace
  "Replacement for `clojure.stacktrace/print-stack-trace` and `print-cause-trace`. These functions are used by `clojure.test`."
  ([tr] (pretty-print-stack-trace tr nil))
  ([tr n]
   (println)
   (print-exception tr {:frame-limit n})))

(defn install-pretty-exceptions
  "Installs an override that outputs pretty exceptions when caught by the main REPL loop. Also, overrides
  `clojure.repl/pst`, `clojure.stacktrace/print-stack-trace`, `clojure.stacktrace/print-cause-trace`.

  Extends `clojure.core/print-method` for type Throwable to print a blank line followed by the
  formatted exception. This allows an expression that evaluates to an exception to be printed prettily,
  but more importantly, ensures that in `clojure.test/is` a failed `thrown-with-msg?` assertion
  prints a formatted exception.

  Finally, installs an [[uncaught-exception-handler]] so that uncaught exceptions in non-REPL threads
  will be printed reasonably.

  Caught exceptions do not print the stack trace; the pretty replacement for `pst` does."
  []
  (reset-var! #'main/repl-caught pretty-repl-caught)
  (reset-var! #'repl/pst pretty-pst)
  (reset-var! #'st/print-stack-trace pretty-print-stack-trace)
  (reset-var! #'st/print-cause-trace pretty-print-stack-trace)

  (defmethod print-method Throwable
    [t ^Writer writer]
    (.write writer ^String System/lineSeparator)
    (.write writer (e/format-exception t)))

  ;; This is necessary due to direct linking (from clojure.test to clojure.stacktrace).
  (RT/loadResourceScript "clojure/test.clj")

  (Thread/setDefaultUncaughtExceptionHandler (uncaught-exception-handler))
  nil)

(defn -main
  "Installs pretty exceptions, then delegates to clojure.main/main."
  {:added "1.3.0"}
  [& args]
  (install-pretty-exceptions)
  (apply main/main args))

(defn main
  "Entrypoint compatible with tools.build.  The :fn argument is a symbol that's resolved to
  a function to invoke, and is passed the argument map."
  {:added "3.5.0"}
  [arg-map]
  (install-pretty-exceptions)
  (let [{delegate-fn :fn} arg-map
        f (or (requiring-resolve delegate-fn)
              (throw (ex-info (str "Could not resolve function to invoke: " delegate-fn)
                              arg-map)))]
    (f (dissoc arg-map :fn))))

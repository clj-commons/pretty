(ns clj-commons.pretty.repl
  "Utilities to assist with REPL-oriented development.

  If you are using Stuart Sierra's component library, you may want to also require
  [[clj-commons.component]]."
  (:require
    [clj-commons.format.exceptions :as e]
    [clojure.main :as main]
    [clojure.repl :as repl]
    [clojure.stacktrace :as st])
  (:import
    (clojure.lang RT)))

(defn- reset-var!
  [v override]
  (alter-var-root v (constantly override)))

(defn- print-exception
  [e options]
  (print (e/format-exception e options))
  (flush))

(defn pretty-repl-caught
  "A replacement for `clojure.main/repl-caught` that prints the exception to `*err*`, without a stack trace or properties."
  [e]
  (print-exception e {:frame-limit 0 :properties false}))

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
  "Used as an override of `clojure.repl/pst` but uses pretty formatting."
  ([] (pretty-pst *e))
  ([e-or-depth]
   (if (instance? Throwable e-or-depth)
     (print-exception e-or-depth nil)
     (pretty-pst *e e-or-depth)))
  ([e depth]
   (binding [*out* *err*]
     (print-exception e {:frame-limit depth}))))

(defn pretty-print-stack-trace
  "Replacement for `clojure.stacktrace/print-stack-trace` and `print-cause-trace`. These functions are used by `clojure.test`."
  ([tr] (pretty-print-stack-trace tr nil))
  ([tr n]
   (println)
   (print-exception tr {:frame-limit n})))

(defn install-pretty-exceptions
  "Installs an override that outputs pretty exceptions when caught by the main REPL loop. Also, overrides
  `clojure.repl/pst`, `clojure.stacktrace/print-stack-trace`, `clojure.stacktrace/print-cause-trace`.

  In addition, installs an [[uncaught-exception-handler]] so that uncaught exceptions in non-REPL threads
  will be printed reasonably.

  Caught exceptions do not print the stack trace; the pst replacement does."
  []
  (reset-var! #'main/repl-caught pretty-repl-caught)
  (reset-var! #'repl/pst pretty-pst)
  (reset-var! #'st/print-stack-trace pretty-print-stack-trace)
  (reset-var! #'st/print-cause-trace pretty-print-stack-trace)

  ;; This is necessary for Clojure 1.8 and above, due to direct linking
  ;; (from clojure.test to clojure.stacktrace).
  (RT/loadResourceScript "clojure/test.clj")

  (Thread/setDefaultUncaughtExceptionHandler (uncaught-exception-handler))
  nil)

(defn -main
  "Installs pretty exceptions, then delegates to clojure.main/main."
  {:added "1.3.0"}
  [& args]
  (install-pretty-exceptions)
  (apply main/main args))

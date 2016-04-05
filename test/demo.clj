(ns demo
  (:use [io.aviso ansi binary exception repl]
        [clojure test pprint])
  (:require [clojure.java.io :as io]
            [criterium.core :as c])
  (:import (java.sql SQLException)))

(install-pretty-exceptions)

(defn- jdbc-update
  []
  (throw (SQLException. "Database failure\nSELECT FOO, BAR, BAZ\nFROM GNIP\nfailed with ABC123" "ABC" 123)))

(defprotocol Worker
  (do-work [this]))

(defn make-jdbc-update-worker
  []
  (reify
      Worker
    (do-work [this] (jdbc-update))))

(defn- update-row
  []
  (try
    (-> (make-jdbc-update-worker) do-work)
    (catch Throwable e
      (throw (RuntimeException. "Failure updating row" e)))))

(defn make-exception
  "Creates a sample exception used to test the exception formatting logic."
  []
  (try
    (update-row)
    (catch Throwable e
      ;; Return it, not rethrow it.
      (RuntimeException. "Request handling exception" e))))

(defn make-ex-info
  ""
  []
  (try
    (throw (make-exception))
    (catch Throwable t
      ;; Return it, not rethrow it.
      (ex-info "Exception in make-ex-info."
               {:function 'make-exception}
               t))))

(defn infinite-loop
  []
  (infinite-loop))

(defn countdown
  [n]
  (if (zero? n)
    (throw (RuntimeException. "Boom!"))
    (countdown (dec n))))

(comment

  ;; 11 Feb 2016 -  553 µs (14 µs std dev) - Clojure 1.8

  (let [out (io/writer "target/output.txt")
        e (make-ex-info)]
    (c/bench (write-exception out e)))

  ;; 11 Feb 2016 - 213 µs (4 µs std dev) - Clojure 1.8

  (let [e (make-ex-info)]
    (c/bench (doseq [x (analyze-exception e nil)]
               (-> x :stack-trace doall))))

  )

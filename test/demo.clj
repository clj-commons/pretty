(ns demo
  (:require
    [io.aviso.repl :as repl]
    [io.aviso.exception :as e]
    io.aviso.component
    [clojure.java.io :as io]
    [criterium.core :as c]
    [clojure.test :refer [report]])
  (:import (java.sql SQLException)))

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

(defn test-failure
  []
  (report {:type :error :expected nil :actual (make-ex-info)}))

(comment

  (repl/install-pretty-exceptions)

  (countdown 10)
  (infinite-loop)
  (throw (make-ex-info))
  (test-failure)

  *clojure-version*

  ;; 11 Feb 2016 -  553 µs (14 µs std dev) - Clojure 1.8
  ;; 13 Sep 2021 -  401 µs (16 µs std dev) - Clojure 1.11.1

  (let [e (make-ex-info)]
    (c/bench (e/format-exception e)))

  ;; 11 Feb 2016 - 213 µs (4 µs std dev) - Clojure 1.8
  ;; 28 Sep 2018 - 237 µs (8 µs std dev) - Clojure 1.9

  (let [e (make-ex-info)]
    (c/bench (doseq [x (e/analyze-exception e nil)]
               (-> x :stack-trace doall))))
  )

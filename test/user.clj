(ns user (import (java.sql SQLException))
  (use [io.aviso ansi binary exception repl]
       [clojure test pprint]))

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

(defn infinite-loop
  []
  (infinite-loop))

(defn countdown
  [n]
  (if (zero? n)
    (throw (RuntimeException. "Boom!"))
    (countdown (dec n))))
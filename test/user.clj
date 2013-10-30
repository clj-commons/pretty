(ns user
  (import (java.sql SQLException))
  (use [io.aviso ansi binary exception]
       [clojure test pprint]))

(defn- jdbc-update
  []
  (throw (SQLException. "Database failure" "ABC" 123)))

(defn- update-row
  []
  (try
    (jdbc-update)
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

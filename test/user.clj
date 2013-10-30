(ns user
  (import (java.sql SQLException))
  (use [io.aviso ansi binary exception]
       [clojure test pprint]))

(defn make-exception
  "Creates a sample exception used to test the exception formatting logic."
  []
  (->> (SQLException. "Database failure" "ABC" 123)
       (RuntimeException. "Failure updating row")
       (RuntimeException. "Request handling exception")))

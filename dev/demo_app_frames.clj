(ns demo-app-frames
  "This namespace demonstrates how customizing clj-commons.format.exceptions/*app-frames*
  helps highlight application logic in stacktraces

  The `comment` block at end of file demonstrates this feature.
  "
  (:require [clj-commons.pretty.repl :as repl]))

(repl/install-pretty-exceptions)

;; -- provided.* namespaces are libraries we're consuming  ---------------------
(ns provided.db
  (:import (java.sql SQLException)))

(defn jdbc-update
  []
  (throw (SQLException. "Database failure" "ABC" 123)))

(ns provided.worker)

(defprotocol Worker
  (do-work [this]))

(ns provided.db-worker)

(defn make-jdbc-update-worker
  []
  (reify
      provided.worker/Worker
    (provided.worker/do-work [this] (provided.db/jdbc-update))))

;; -- my-app are namespaces belonging to our application  ----------------------
(ns my-app.db)

(defn update-row
  []
  (try
    (-> (provided.db-worker/make-jdbc-update-worker) provided.worker/do-work)
    (catch Throwable e
      (throw (RuntimeException. "Failure updating row" e)))))


(ns my-app.handler)

(defn make-exception
  "Creates a sample exception used to test the exception formatting logic."
  []
  (try
    (my-app.db/update-row)
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


(ns my-app.handler-test
  (:require [clojure.test :refer [report]]))

(defn test-failure
  []
  (report {:type :error :expected nil :actual (my-app.handler/make-ex-info)}))

(comment
  ;; Run these commands in a REPL
  (require '[demo-appframes] :reload)

  ;; Should show no app-frames highlighted
  (alter-var-root #'clj-commons.format.exceptions/*app-frame-names* (constantly []))
  (my-app.handler-test/test-failure)

  ;; Should show app-frames (beginning with my-app) highlighted
  (alter-var-root #'clj-commons.format.exceptions/*app-frame-names* (constantly [#"my-app.*"]))
  (my-app.handler-test/test-failure)

  )

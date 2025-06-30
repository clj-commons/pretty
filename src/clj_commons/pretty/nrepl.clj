(ns clj-commons.pretty.nrepl
  "Middleware to setup pretty exception reporting in nREPL."
  {:added "3.5.0"}
  (:require [clj-commons.pretty.repl :as repl]
            [nrepl.middleware.caught :as nc]))

(defn install
  "Returns handler, unchanged, after enabling pretty exceptions."
  [handler]
  (repl/install-pretty-exceptions)
  (set! nc/*caught-fn* repl/pretty-pst)
  handler)

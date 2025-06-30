(ns clj-commons.pretty.nrepl
  "Middleware to setup pretty exception reporting in nREPL."
  {:added "3.5.0"}
  (:require [clj-commons.pretty.repl :as repl]
            [nrepl.middleware :as middleware]
            [nrepl.middleware.caught :as caught]))

(defn wrap-pretty
  [handler]
  (repl/install-pretty-exceptions)
  (fn with-pretty
    [msg]
    (handler (assoc msg ::caught/caught `repl/pretty-repl-caught))))

(middleware/set-descriptor! #'wrap-pretty
                            {:doc      (-> #'wrap-pretty meta :doc)
                             :handles  {}
                             :requires #{}
                             :expects  #{"eval" #'caught/wrap-caught}})

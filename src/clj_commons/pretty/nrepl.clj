(ns clj-commons.pretty.nrepl
  "Middleware to setup pretty exception reporting in nREPL."
  {:added "3.5.0"}
  (:require [clj-commons.pretty.repl :as repl]
            [nrepl.middleware :as middleware]
            [nrepl.middleware.caught :as caught]))

(defn wrap-pretty
  [handler]
  "Ensures that exceptions are printed using pretty, including uncaught REPL exceptions.

  This sets the message key :nrepl.middleware.caught/caught, if not previously set."
  (repl/install-pretty-exceptions)
  (fn with-pretty
    [msg]
    (let [msg' (if (contains? msg ::caught/caught)
                 msg
                 (assoc msg ::caught/caught `repl/pretty-repl-caught))]
      (handler msg'))))

(middleware/set-descriptor! #'wrap-pretty
                            {:doc      (-> #'wrap-pretty meta :doc)
                             :handles  {}
                             :requires #{}
                             :expects  #{#'caught/wrap-caught}})

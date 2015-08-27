(ns pretty.plugin
  "A plugin for Leiningen that automatically enables pretty printing."
  {:added "0.1.19"}
  (:require [io.aviso.repl :as repl]))

(defn hooks
  "Enables pretty printing of exceptions during the session. This is evaluated in the Leinigen classpath, not the project's
  (if any)."
  []
  (repl/install-pretty-exceptions))

(defn middleware
  "Automatically adds the nREPL middleware that enables Pretty."
  [project]
  (update-in project [:repl-options :nrepl-middleware]
             (fnil conj [])
             'io.aviso.nrepl/pretty-middleware))


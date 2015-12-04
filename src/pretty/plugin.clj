(ns pretty.plugin
  "A plugin for Leiningen that automatically enables pretty printing."
  {:added "0.1.19"}
  (:require [io.aviso.repl :as repl]))

(defn hooks
  "Enables pretty printing of exceptions within Leiningen. This is evaluated in the Leinigen classpath, not the project's
  (if any)."
  []
  (repl/install-pretty-exceptions))

(defn middleware
  "Automatically adds the :injections that enable Pretty."
  [project]
  (update-in project [:injections]
             (fnil into [])
             ['(require 'io.aviso.repl)
              '(io.aviso.repl/install-pretty-exceptions)]))


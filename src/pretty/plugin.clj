(ns pretty.plugin
  "A plugin for Leiningen that automatically enables pretty printing."
  {:added "0.1.19"}
  (:require [io.aviso.repl :as repl]
            [leiningen.core.main :as main]))

(defn hooks
  "Enables pretty printing of exceptions within Leiningen. This is evaluated in the Leinigen classpath, not the project's
  (if any)."
  []
  (repl/install-pretty-exceptions))


;; Ugly! But necessary since middleware gets invoked more than once for some unknown reason.
(def ^:private print-warning
  (delay (main/warn "Unable to enable pretty exception reporting, as io.aviso/pretty is not a project dependency.")))

(defn middleware
  "Automatically adds the :injections that enable Pretty."
  [project]
  (if (contains? (->> project
                      :dependencies
                      (map first)
                      set)
                 'io.aviso/pretty)
    (update-in project [:injections]
               conj
               `(try
                  (require 'io.aviso.repl)
                  (let [install# (resolve 'io.aviso.repl/install-pretty-exceptions)]
                    (install#))
                  (catch Throwable t#
                    (println "Error loading io.aviso/pretty support:"
                             (or (.getMessage t#)
                                 (type t#))))))
    (do @print-warning project)))


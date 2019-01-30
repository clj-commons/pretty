(ns io.aviso.lein-pretty
  "A plugin for Leiningen that automatically enables pretty printing."
  {:added "0.1.19"}
  (:require
    [leiningen.core.main :as main]))

;; Ugly! But necessary since middleware gets invoked more than once for some unknown reason.
(def ^:private print-warning
  (delay (main/warn "Unable to enable pretty exception reporting, as io.aviso/pretty is not a project dependency.")))

(defn inject
  "Adds the :injections that enable Pretty inside the project's process, by executing
  `(io.aviso.repl/install-pretty-exceptions)`.

  This is enabled by adding `io.aviso.lein-pretty/inject` to the :middleware of the project.clj."
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
    (do
      @print-warning
      project)))


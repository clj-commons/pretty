(ns pretty.plugin
  "A plugin for Leiningen that automatically enables pretty printing."
  {:added "0.1.19"}
  (:require [io.aviso.repl :as repl]
            [io.aviso.exception :refer [format-exception]]
            [leiningen.core.main :refer [info warn]]
            [leiningen.core.eval :refer [eval-in-project]]))

(defn hooks
  "Enables pretty printing of exceptions during the session. This is evaluated in the Leinigen classpath, not the project's
  (if any)."
  []
  (info "Installing Pretty for Leiningen.")
  (repl/install-pretty-exceptions))

(defn middleware
  "Sneaky: get the project so that we can do an eval-in-project, and return the project unchanged."
  [project]

  ;; Probably could do a check here to see if pretty is a (transitive) dependency; likewise
  ;; logging.
  (info "Installing Pretty for project.")

  (try
    (eval-in-project project
                     '(do
                        (require '[io.aviso.repl :as repl])
                        (repl/install-pretty-exceptions)))
    (catch Throwable t
      (warn (str
              "Unable to install Pretty exceptions in project.\n"
              (format-exception t)))))

  project)


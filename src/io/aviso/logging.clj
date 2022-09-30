(ns io.aviso.logging
  "Provides functions that hook into clojure.tools.logging to make use of Pretty to format exceptions.

  You must [add clojure.tools.logging as an explicit dependency](https://github.com/clojure/tools.logging) of your project."
  {:added "0.1.15"}
  (:require [clojure.tools.logging :as l]
            [io.aviso.exception :as e])
  (:import [java.lang Thread$UncaughtExceptionHandler]))

(def ^:private eol (System/getProperty "line.separator"))

(defn- install-pretty-logging*
  [options]
  (alter-var-root
    #'l/log*
    (fn [default-impl]
      (fn [logger level throwable message]
        (default-impl logger
                      level
                      nil
                      (if throwable
                        (str message
                             eol
                             (e/format-exception throwable options))
                        message))))))

(defn install-pretty-logging
  "Modifies clojure.tools.logging to use pretty exception logging."
  ([]
   (install-pretty-logging* nil))
  ([frame-filter-fn]
   (install-pretty-logging* {:filter frame-filter-fn})))

(defn uncaught-exception-handler
  "Creates a reified UncaughtExceptionHandler that uses clojure.tools.logging/error, rather than
  simply printing the exception, which is the default behavior."
  []
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException [_ _ t]
      (l/error t (or (.getMessage t) (-> t .getClass .getName))))))

(defn install-uncaught-exception-handler
  "Installs a default UncaughtExceptionHandler. "
  []
  (Thread/setDefaultUncaughtExceptionHandler (uncaught-exception-handler)))

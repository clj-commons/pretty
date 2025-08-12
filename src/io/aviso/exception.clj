(ns io.aviso.exception
  "A shim in org.clj-commons/pretty to emulate a few key functions from io.aviso/pretty.

  This namespace may be deleted in a future release."
  {:added "2.5.0"
   :deprecated "2.5.0"}
  (:require [clj-commons.format.exceptions :as e]))

(defn demangle
  [^String s]
  (e/demangle s))

(defn format-exception
  ([exception]
   (e/format-exception exception))
  ([exception options]
   (e/format-exception exception options)))

(defn write-exception
  ([exception]
   (e/print-exception exception))
  ([exception options]
   (e/print-exception exception options)))

(def ^:dynamic *default-frame-rules*
  "This is provided only to prevent source compilation failures; changing this value does not affect
  how the clj-commons.format.exceptions namespace operates."
  [])

(def ^:dynamic *fonts*
  "This is provided only to prevent source compilation failures; changing this value does not affect
  how the clj-commons.format.exceptions namespace operates."
  {})

(def ^{:dynamic true
       :added "3.6.3"
       :deprecated "3.6.3"} *traditional*
  "This is provided only to prevent source compilation failures; changing this value does not affect
    how the clj-commons.format.exceptions namespace operates."
  false)

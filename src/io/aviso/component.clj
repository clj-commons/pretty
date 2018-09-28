(ns io.aviso.component
  "Changes exception output for SystemMap and Lifecycle, from
  [Stuart Sierra's Component library](https://github.com/stuartsierra/component), to be shorter placeholders.

  SystemMap instances print as `#<SystemMap>`.

  Lifecycle instances print as `#<Component CLASS>` (where `CLASS` is name of the record class).

  Without these changes, exception output for system maps produces volumes of deeply nested
  and redundant data."
  {:added "0.1.35"}
  (:require
    com.stuartsierra.component
    [io.aviso.exception :refer [exception-dispatch]])
  (:import
    (com.stuartsierra.component SystemMap Lifecycle)))

(defmethod exception-dispatch SystemMap
  [_]
  (print "#<SystemMap>"))

(defmethod exception-dispatch Lifecycle
  [this]
  (print (str "#<Component " (-> this class .getName) ">")))


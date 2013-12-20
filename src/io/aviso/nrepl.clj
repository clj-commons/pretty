(ns io.aviso.nrepl
  "nREPL middleware to enable pretty exception reportinging in the REPL."
  (:use [io.aviso.repl]))

(defn pretty-middleware
  "nREPL middleware that simply ensures that pretty exception reporting is installed."
  [handler]
  (install-pretty-exceptions)
  handler)
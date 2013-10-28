(ns io.aviso.writer
  "The Writer protocol is used as the target of any written output.")

(defprotocol Writer
  "May receive strings, which are printed, or stored."

  (write [this string] "Writes the string to the Writer."))

(extend-protocol Writer
  ;; Appendable is implemented by StringBuilder and PrintWriter.
  Appendable
  (write [this string] (.append this string)))

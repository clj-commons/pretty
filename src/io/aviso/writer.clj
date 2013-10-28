(ns io.aviso.writer
  "The Writer protocol is used as the target of any written output.")

(defprotocol Writer
  "May receive strings, which are printed, or stored."

  (write [this string] "Writes the string to the Writer."))

(extend-protocol Writer
  ;; Appendable is implemented by StringBuilder and PrintWriter.
  Appendable
  (write [this ^CharSequence string] (.append this string)))

(defn writes
  "Constructs a string from the values (with no seperator) and writes the string to the Writer.

  This is used to get around the fact that protocols do not support varadic parameters."
  [writer & values]
  (write writer (apply str values)))
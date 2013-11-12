(ns io.aviso.writer
  "The Writer protocol is used as the target of any written output.")

(defprotocol Writer
  "May receive strings, which are printed, or stored.

  Writer is extended onto java.lang.Appendable, a common interface implemented by both PrintWriter and StringBuilder (among
  many others)"

  (write-string [this string] "Writes the string to the Writer."))

(extend-protocol Writer
  ;; Appendable is implemented by StringBuilder and PrintWriter.
  Appendable
  (write-string [this ^CharSequence string] (.append this string)))

(def endline
  "End-of-line terminator, platform specific."
  (System/getProperty "line.separator"))

(defn write
  "Constructs a string from the values (with no seperator) and writes the string to the Writer.

  This is used to get around the fact that protocols do not support varadic parameters."
  ([writer value]
   (write-string writer (str value)))
  ([writer value & values]
   (doseq [value values]
     (write-string writer (str value)))))

(defn writeln
  "Constructs a string from the values (with no seperator) and writes the string to the Writer,
  followed by an end-of-line terminator."
  ([writer]
   (write-string writer endline))
  ([writer & values]
   (write writer values)
   (write-string writer endline)))

(defn writef
  "Writes formatted data."
  [writer fmt & values]
  (write-string writer (apply format fmt values)))

(defn into-string
  "Creates a StringBuilder and passes that as the first parameter to the function, along with the other parameters.

  Returns the value of the StringBuilder after invoking the function."
  [f & params]
  (let [sb (StringBuilder. 2000)]
    (apply f sb params)
    (.toString sb)))


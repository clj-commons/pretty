(ns io.aviso.columns
  "Assistance for formatting data into columns. Each columns has a width, and data within the column
  may be left or right justified. Generally, columns are sized to the largest item in the column.
  When a value is provided in a column, it may be associated with an explicit width which is helpful
  when the value contains non-printing characters (such as those defined in the io.aviso.ansi namespace)."
  (:require [io.aviso.writer :as w]))

(defn- string-length [^String s] (.length s))

(defn- decompose
  [column-value]
  (if (vector? column-value)
    ;; Ensure that the column value is, in fact, a string.
    [(nth column-value 0) (-> (nth column-value 1) str)]
    (let [as-string (str column-value)]
      [(string-length as-string) as-string])))

(defn- indent
  "Indents sufficient to pad the column value to the column width."
  [writer indent-amount]
  (w/write writer (apply str (repeat indent-amount \space))))

(defn- truncate
    [justification ^String string amount]
  (cond
    (nil? amount) string
    (zero? amount) string
    (= :left justification) (.substring string 0 (- (string-length string) amount))
    (= :right justification) (.substring string amount)
    :else string))

(defn- write-column-value
  [justification width]
  (fn column-writer [writer column-value]
    (let [[value-width value-string] (decompose column-value)
          indent-amount (and width (max 0 (- width value-width)))
          truncate-amount (and width (max 0 (- value-width width)))
          truncated (truncate justification value-string truncate-amount)]
      (if (and indent-amount (= justification :right))
        (indent writer indent-amount))
      (w/write writer truncated)
      (if (and indent-amount (= justification :left))
        (indent writer indent-amount)))))

(defn- fixed-column
  [fixed-value]
  (fn [writer column-data]
    (w/write writer fixed-value)
    column-data))

(defn- dynamic-column
  "Returns a function that consumes the next column data value and delegates to a column writer function
  to actually write the output for the column."
  [column-writer]
  (fn [writer [column-value & remaining-column-data]]
    (column-writer writer column-value)
    remaining-column-data))

(defn- nil-column
  "Does nothing and returns the column data unchanged."
  [writer column-data]
  column-data)

(defn- column-def-to-fn [column-def]
  (cond
    (string? column-def) (fixed-column column-def)
    (number? column-def) (-> (write-column-value :left column-def) dynamic-column)
    (nil? column-def) nil-column
    (= :none column-def) (-> (write-column-value :none nil) dynamic-column)
    :else (-> (apply write-column-value column-def) dynamic-column)))

(defn format-columns
  "Converts a number of column definitions into a formatting function. Each column definition may be:

  - a string, to indicate a non-consuming column that outputs a fixed value. This is often just a space
  character or two, to seperate columns.
  - a number, to indicate a consuming column that outputs a left justified value of the given width.
  - a vector containing a keyword and a number; the number is the width, the keyword is the justification.
  - :none, to indicate a consuming column with no explicit width
  - nil, which is treated like an empty string

  With explicit justification, the keyword may be :left, :right, or :none.  :left justification
  pads the column with spaces after the value; :right justification pads the column with spaces
  before the value. :none does not pad with spaces at all, and should only be used in the final column.

  :left and :right justified columns will truncate long values; :left truncates from the right (e.g.,
  display initial characters, discard trailing characters)
  and :right truncates from the left (e.g., discards initial characters, display trailing characters).
  Generally speaking, truncation does not occur because columns are sized to fit their contents.

  Values are normally strings, but to support non-printing characters in the strings, a value may
  be a two-element vector consisting of its effective width and the actual value to write. Non-string
  values are converted to strings using str.

  The returned function accepts a Writer and the column data and writes each column value, with appropriate
  padding, to the Writer."
  [& column-defs]
  (let [column-fns (map column-def-to-fn column-defs)]
    (fn [writer & column-data]
      (loop [column-fns column-fns
             column-data column-data]
        (if (empty? column-fns)
          (w/writeln writer)
          (let [cf (first column-fns)
                remaining-column-data (cf writer column-data)]
            (recur (rest column-fns) remaining-column-data)))))))

(defn write-rows
  "A convienience for writing rows of columns using a prepared column formatter.

  - writer - target of output
  - column-formatter - formatter function created by format-columns
  - extractors - seq of functions that extract a column value from a row; typically a keyword when the row is a map
  - row-data - a seq of row data"
  [writer column-formatter extractors row-data]
  (let [column-data-extractor (apply juxt extractors)]
    (doseq [row row-data]
      (apply column-formatter writer (column-data-extractor row)))))



(ns io.aviso.columns
  "Assistance for formatting data into columns. Each columns has a width, and data within the column
  may be left or right justified. Generally, columns are sized to the largest item in the column.
  When a value is provided in a column, it may be associated with an explicit width which is helpful
  when the value contains non-printing characters (such as those defined in the io.aviso.ansi namespace)."
  (:require [io.aviso.writer :as w]))

(defn- string-length [^String s] (.length s))

(defn- data-width [column-value]
  (if (vector? column-value)
    (get column-value 0)
    (-> column-value str string-length)))

(defn- data-value [column-value]
  (if (vector? column-value)
    (get column-value 1)
    column-value))

(defn- indent
  "Indents sufficient to pad the column value to the column width."
  [writer column-width column-value]
  (w/write writer (apply str (repeat (- column-width (data-width column-value)) \space))))

(defn- format-column
  [width justification]
  (fn [writer column-value]
    (if (= justification :right)
      (indent writer width column-value))
    (w/write writer (data-value column-value))
    (if (= justification :left)
      (indent writer width column-value))))

(defn- fixed-column
  [fixed-value]
  (fn [writer column-data]
    (w/write writer fixed-value)
    column-data))

(defn- dynamic-column
  [column-formatter]
  (fn [writer [column-value & remaining-column-data]]
    (column-formatter writer column-value)
    remaining-column-data))

(defn- nil-column
  "Does nothing and returns the column-data unchanged."
  [writer column-data]
  column-data)

(defn- column-def-to-fn [column-def]
  (cond
    (string? column-def) (fixed-column column-def)
    (number? column-def) (-> (format-column column-def :left) dynamic-column)
    (nil? column-def) nil-column
    :else (let [[justification width] column-def]
            (-> (format-column width justification) dynamic-column))))

(defn format-columns
  "Converts a number of column definitions into a formatting function. Each column definition may be:

  - a string, to indicate a non-consuming column that outputs a fixed value. This is often just a space
  character or two, to seperate columns.
  - a number, to indicate a consuming column that outputs a left justified value of the given width.
  - a vector containing a keyword and a number; then number is the width, the keyword is the justification.
  - nil, which is treated like an empty string

  With explicit justification, the keyword may be :left, :right, or :none.  :left justification
  pads the column with spaces after the value; :right justification pads the column with spaces
  before the value. :none does not pad with spaces at all, and should only be used in the final column.

  Values are normally strings, but to support non-printing characters in the strings, a value may
  be a two-element vector consisting of its effective width and the actual value to print.

  The returned function accepts a Writer and the column data and writes each column value, with appropriate
  padding, to the Writer. No end-of-line character is written. "
  [& column-defs]
  (let [column-fns (map column-def-to-fn column-defs)]
    (fn [writer & column-data]
      (loop [column-fns column-fns
             column-data column-data]
        (when-not (empty? column-fns)
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



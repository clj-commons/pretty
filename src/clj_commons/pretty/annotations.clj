(ns clj-commons.pretty.annotations
  "Tools to annotate a line of source code, in the form of callouts (lines and arrows) connected to a message.

      SELECT DATE, AMT FROM PAYMENTS WHEN AMT > 10000
                   ▲▲▲               ▲▲▲▲
                   │                 │
                   │                 └╴ Unknown token
                   │
                   └╴ Invalid column name

  This kind of output is common with various kinds of parsers or interpreters.

  Specs for types and functions are in the [[spec]] namespace."
  {:added "3.3.0"})

(def default-style
  "The default style used when generating callouts.

  Key       | Default | Description
  ---       |---      |---
  :font     | :yellow | Default font characteristics if not overrided by annotation
  :spacing  | :compact| One of :tall, :compact, or :minimal
  :marker   | \"▲\"   | The marker used to identify the offset/length of an annotation
  :bar      | \"│\"   | Character used as the vertical bar in the callout
  :nib      | \"└╴ \" | String used just before the annotation's message

  When :spacing is :minimal, only the lines with markers or error messages appear
  (the lines with just vertical bars are omitted).  :compact spacing is the same, but
  one line of bars appears between the markers and the first annotation message.

  The :marker is used to build a string that matches the :length of the callout.

  :marker can be a single string, which is repeated.

  :marker can be a three-character string.  The middle character is repeated
  to pad the marker to the necessary length.

  :marker can also be a function; the function is passed the length
  and returns a string (or composed string) that must be that number of characters wide.

  Note: rendering of Unicode characters in HTML often uses incorrect fonts or adds unwanted
  character spacing; the annotations look proper in console output."
  {:font :yellow
   :spacing :compact
   :marker "▲"
   :bar "│"
   :nib "└╴ "})

(def ^:dynamic *default-style*
  "The default style used when no style is provided; some applications may bind or
   override this."
  default-style)

(defn- nchars
  [n ch]
  (apply str (repeat n ch)))

(defn- split-marker
  [marker length]
  (let [[l m r] marker
        b (StringBuilder. (int length))]
    (.append b ^char l)
    (dotimes [_ (- length 2)]
      (.append b ^char m))

    (when (> length 1)
      (.append b ^char r))

    (.toString b)))

(defn- extend-marker
  [marker length]
  (cond
    (fn? marker)
    (marker length)

    (not (string? marker))
    (throw (ex-info "Marker should be a function or a string"
                    {:marker marker}))

    (= 1 (count marker))
    (nchars length marker)

    (= 3 (count marker))
    (split-marker marker length)

    :else
    (throw (ex-info "Marker string must be 1 or 3 characters"
                    {:marker marker}))))

(defn- markers
  [style annotations]
  (let [{:keys          [font]
         default-marker :marker} style]
    (loop [output-offset 0
           annotations annotations
           result [font]]
      (if-not annotations
        result
        (let [{:keys [offset length font marker]
               :or   {length 1
                      marker default-marker}} (first annotations)
              spaces-needed (- offset output-offset)
              result' (conj result
                            (nchars spaces-needed \space)
                            [font (extend-marker marker length)])]
          (recur (long (+ offset length))
                 (next annotations)
                 result'))))))

(defn- bars
  [style annotations]
  (let [{:keys [font bar]} style]
    (loop [output-offset 0
           annotations annotations
           result [font]]
      (if-not annotations
        result
        (let [{:keys [offset font]} (first annotations)
              spaces-needed (- offset output-offset)
              result' (conj result
                            (nchars spaces-needed \space)
                            [font bar])]
          (recur (long (+ offset 1))
                 (next annotations)
                 result'))))))

(defn- bars+message
  [style annotations]
  (let [{:keys [font bar nib]} style]
    (loop [output-offset 0
           [annotation & more-annotations] annotations
           result [font]]
      (let [{:keys [offset font message]} annotation
            spaces-needed (- offset output-offset)
            last? (not (seq more-annotations))
            result' (conj result
                          (nchars spaces-needed \space)
                          [font
                           (if last?
                             nib
                             bar)
                           (when last?
                             message)])]
        (if last?
          result'
          (recur (long (+ offset 1))
                 more-annotations
                 result'))))))

(defn callouts
  "Creates callouts (the marks, bars, and messages from the example) from annotations.

  Each annotation is a map:

  Key       | Description
  ---       |---
  :message  | Composed string of the message to present
  :offset   | Integer position (from 0) to mark on the line
  :length   | Number of characters in the marker (min 1, defaults to 1)
  :font     | Override of the style's font; used for marker, bars, nib, and message
  :marker   | Override of the style's marker

  The leftmost column has offset 0; some frameworks may report this as column 1
  and an adjustment is necessary before invoking callouts.

  At least one annotation is required; they will be sorted into an appropriate order.
  Annotation's ranges should not overlap.

  The messages should be relatively short, and not contain any line breaks.

  Returns a sequence of composed strings, one for each line of output.

  The calling code is responsible for any output; even the line being annotated;
  this might look something like:

      (ansi/perr source-line)
      (run! ansi/perr (annotations/annotate annotations))

  Uses the style defined by [[*default-style*]] if no style is provided."
  ([annotations]
   (callouts  *default-style* annotations))
  ([style annotations]
   ;; TODO: Check for overlaps
   (let [expanded (sort-by :offset annotations)
         {:keys [spacing]} style
         marker-line (markers style expanded)]
     (loop [annotations expanded
            first? true
            result [marker-line]]
       (let [include-bars? (or (= spacing :tall)
                               (and first? (= spacing :compact)))
             result' (conj result
                           (when include-bars?
                             (bars style annotations))
                           (bars+message style annotations))
             annotations' (butlast annotations)]
         (if (seq annotations')
           (recur annotations' false result')
           (remove nil? result')))))))

(defn annotate-lines
  "Intersperses numbered lines with callouts to form a new sequence
  of composable strings where input lines are numbered, and
  callout lines are indented beneath the input lines.

  Example:

  ```
  1: SELECT DATE, AMT
            ▲▲▲
            │
            └╴ Invalid column name
  2: FROM PAYMENTS WHEN AMT > 10000
                   ▲▲▲▲
                   │
                   └╴ Unknown token
  ```
  Each line is a map:

  Key          | Value
  ---          |---
  :line        | Composed string for a single line of input (usually, just a string)
  :annotations | Optional, a seq of annotation maps (used to create the callouts)

  Option keys are all optional:

  Key                | Value
  ---                |---
  :style             | style map (for callouts), defaults to [*default-style*]
  :start-line        | Defaults to 1
  :line-number-width | Width for the line numbers column

  The :line-number-width option is usually computed from the maximum line number
  that will be output.

  Returns a seq of composed strings, each representing a single line of output.
  For example, `(run! ansi/perr (annotation-lines ...))`.

  "
  ([lines]
   (annotate-lines nil lines))
  ([opts lines]
   (let [{:keys [style start-line]
          :or   {style      *default-style*
                 start-line 1}} opts
         max-line-number (+ start-line (count lines) -1)
         ;; inc by one to account for the ':'
         line-number-width (inc (or (:line-number-width opts)
                                    (-> max-line-number str count)))
         callout-indent (repeat (nchars (inc line-number-width) " "))]
     (loop [[line-data & more-lines] lines
            line-number start-line
            result []]
       (if-not line-data
         result
         (let [{:keys [line annotations]} line-data
               callout-lines (when (seq annotations)
                               (callouts style annotations))
               result' (cond-> (conj result
                                     (list
                                       [{:width line-number-width}
                                        line-number ":"]
                                       " "
                                       line))
                               callout-lines (into
                                               (map list callout-indent callout-lines)))]
           (recur more-lines (inc line-number) result')))))))

(defn underline-marker
  "Marker function that renders as a heavy underline."
  {:added "3.5.0"}
  [^long length]
  (str "┯" (nchars (dec length) "━")))


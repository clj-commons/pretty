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
  {:added "3.3.0"}
  (:require [clj-commons.ansi :as ansi]))

(def default-style
  "The default style used when generating callouts.

  Key       | Default | Description
  ---       |---      |---
  :font     | :yellow | Default font characteristics if not overrided by annotation
  :spacing  | :compact| One of :tall, :compact, or :minimal
  :marker   | \"▲\"   | The marker used to identify the offset/length of an annotation
  :bar      | \"│\"   | Character used as the vertical bar in the callout
  :nib      | \"└╴ \" | String used just before the annotation's message
  :bend     | \"┌─┘\" | String used when changing the column of an annotation

  When :spacing is :minimal, only the lines with markers or error messages appear
  (the lines with just vertical bars are omitted).  :compact spacing is the same, but
  one line of bars appears between the markers and the first annotation message.

  Note the _intentional_ space at the end of the default :nib value.

  The :marker is used to build a string that matches the :length of the callout.

  :marker can be a single string, which is repeated.

  :marker can be a three-character string.  The middle character is repeated
  to pad the marker to the necessary length.

  :marker can also be a function; the function is passed the length
  and returns a string (or composed string) that must be that number of characters wide.

  :bend is used when the column for an annotation must be rerouted; like a marker, it
  is a three character string that may be extended by repeating the middle character.

  :bar, :nib, and :bend must be simple, not composed, strings.

  Note: rendering of Unicode characters in HTML often uses incorrect fonts or adds unwanted
  character spacing; the annotations look proper in console output."
  {:font    :yellow
   :spacing :compact
   :marker  "▲"
   :bar     "│"
   :nib     "└╴ "
   :bend    "┌─┘"})

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
           annotations   annotations
           result        [font]]
      (if-not annotations
        result
        (let [{:keys [offset length font marker]
               :or   {length 1
                      marker default-marker}} (first annotations)
              spaces-needed (- offset output-offset)
              result'       (conj result
                                  (nchars spaces-needed \space)
                                  [font (extend-marker marker length)])]
          (recur (long (+ offset length))
                 (next annotations)
                 result'))))))

(defn- line-emitter
  "Create a line emitter. Get the final emitted line, as a vector, with emitted-line."
  [font]
  {:line   [font]
   :offset 0})

(defn- emit-final
  "A vector (starting with a font) of the line (a composed string)."
  [emitter]
  (:line emitter))

(defn- emit-text
  "Emit text onto the line; offset must be greater than the current offset (text must be
  added in offset order). "
  [emitter offset font text]
  (let [{current-offset :offset} emitter
        ;; TODO: Handle when there's an overlap in offsets
        spaces-needed (- offset current-offset)
        spaces        (nchars spaces-needed \space)
        text'         (if font
                        [font text]
                        text)]
    (-> emitter
        (assoc :offset (+ offset (count text)))
        (update :line conj spaces text'))))

(defn- emit-composed
  ([emitter text]
   ;; No :offset adjustment since this should be last and we don't know the composed text's length.
   (update emitter :line conj text))
  ([emitter offset text]
   (let [{current-offset :offset} emitter
         spaces-needed (- offset current-offset)
         spaces        (nchars spaces-needed \space)]
     (update emitter :line conj spaces text))))

(defn- bars
  [{:keys [font bar]} annotations]
  (-> (reduce
        (fn [emitter {:keys [offset font]}]
          (emit-text emitter offset font bar))
        (line-emitter font)
        annotations)
      emit-final))

(defn- annotation-message-lines
  "Produces a composed string of the vertical bars for annotations and the nib and message
  for the final annotation. The message is wrapped and may consume multiple lines.
  Returns a seq of composed strings, one composed string per line."
  [style annotations width]
  (let [{:keys      [bar nib]
         style-font :font} style
        last-annotation (last annotations)
        nib-width       (count nib)
        remaining-width (- width (:offset last-annotation) nib-width -1)
        message-lines   (ansi/wrap remaining-width [(:font last-annotation)
                                                    (:message last-annotation)])]
    (map-indexed (fn [i message-line]
                   (emit-final
                     (reduce (fn [emitter {:keys [offset font] :as annotation}]
                               (cond
                                 (not= last-annotation annotation)
                                 ;; Not the last annotation, just emit the bar at
                                 ;; the right offset.
                                 (emit-text emitter offset font bar)

                                 ;; Last annotation.  First line (which includes the nib)?

                                 (zero? i)
                                 (-> emitter
                                     (emit-text offset font nib)
                                     (emit-composed message-line))

                                 :else
                                 (emit-composed emitter (+ offset nib-width) message-line)))
                             (line-emitter style-font)
                             annotations)))
                 message-lines)))

(defn- reroute
  [style annotations]
  ;; Bars are limited to single characters, so we can space them out by two.
  (let [old-offsets  (map :offset annotations)
        {:keys      [bar bend]
         style-font :font} style
        annotations' (->> annotations
                          (map-indexed (fn [i annotation]
                                         (assoc annotation :offset (* i 2))))
                          vec)
        bounds       (-> annotations count range)
        lines        (map (fn [i]                           ; i = row / line
                            (emit-final
                              (reduce (fn [emitter j]       ; j = annotation within line
                                        (let [{:keys      [font]
                                               new-offset :offset} (nth annotations' j)
                                              old-offset (nth old-offsets j)]
                                          (cond
                                            (= i j)
                                            (emit-text emitter
                                                       new-offset
                                                       font
                                                       (split-marker bend (- old-offset new-offset -1)))

                                            (< j i)
                                            (emit-text emitter
                                                       new-offset
                                                       font
                                                       bar)

                                            :else
                                            (emit-text emitter
                                                       old-offset
                                                       font
                                                       bar))))
                                      (line-emitter style-font)
                                      bounds)))
                          bounds)]
    [annotations' lines]))

(defn callouts
  "Creates callouts (the marks, bars, and messages from the example) from annotations.

  Each annotation is a map:

  Key       | Description
  ---       |---
  :message  | Composed string of the message to present
  :offset   | Integer position (from 0) to mark on the line
  :length   | Number of characters in the marker (min 1, defaults to 1)
  :font     | Override of the style's font; used for marker, bars, nib, bend, and message
  :marker   | Override of the style's marker

  The leftmost column has offset 0; some frameworks may report this as column 1
  and an adjustment is necessary before invoking callouts.

  At least one annotation is required; they will be sorted into an appropriate order.
  Annotation's ranges should not overlap.

  Messages should be short, or will be word-wrapped if the :width option is provided.

  Options:

  Key            | Description
  ---            |---
  :width         | Total width to wrap annotations
  :reroute-width | Minimum width to *avoid* rerouting.  Default: 30

  Rerouting: if the rightmost annotation has less than :reroute-width characters for the
  annotation text, then the annotation columns are rerouted to columns 0, 2, 4, etc.  The
  :bend (from the style) is extended and used to connect the old annotaton column with its new assigned column.

  Returns a sequence of composed strings, one for each line of output.

  The calling code is responsible for any output; even the line being annotated;
  this might look something like:

      (ansi/perr source-line)
      (run! ansi/perr (annotations/annotate annotations))

  Uses the style defined by [[*default-style*]] if no style is provided."
  ([annotations]
   (callouts *default-style* annotations))
  ([style annotations]
   (callouts style annotations nil))
  ([style annotations {:keys [width reroute-width]
                       :or   {width         Long/MAX_VALUE
                              reroute-width 30}}]
   (let [sorted-annotations (vec (sort-by :offset annotations))
         {:keys [spacing]} style
         marker-line        (markers style sorted-annotations)
         available-width    (- width
                               (-> sorted-annotations last :offset)
                               (-> style :nib count))
         reroute?           (< available-width reroute-width)
         [sorted-annotations' reroute-lines] (if reroute?
                                               (reroute style sorted-annotations)
                                               [sorted-annotations nil])
         header             (-> [marker-line
                                 (when (= spacing :compact)
                                   ;; Intentional: bars for the annotations before the :offset is
                                   ;; reworked due to reroute
                                   (bars style sorted-annotations))]
                                (into reroute-lines))]
     (loop [annotations sorted-annotations'
            result      header]
       (let [spacer       (when (= spacing :tall)
                            (bars style annotations))
             result'      (-> result
                              (conj spacer)
                              (into (annotation-message-lines style annotations width)))
             annotations' (butlast annotations)]
         (if (seq annotations')
           (recur annotations' result')
           ;; Important that the vector is converted to a list by remove:
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
  :line-number-width | Width for the line numbers column, or 0 for no line numbers

  The :line-number-width option is usually computed from the maximum line number
  that will be output.

  Returns a seq of composed strings, each representing a single line of output.
  For example, `(run! ansi/perr (annotation-lines ...))`.

  This is an older function, the new annotation-set approach is easier."
  ([lines]
   (annotate-lines nil lines))
  ([opts lines]
   (let [{:keys [style start-line line-number-width]
          :or   {style      *default-style*
                 start-line 1}} opts
         line-numbering?    (not= line-number-width 0)
         ;; inc by one to account for the ':'
         line-number-width' (when line-numbering?
                              (inc (or line-number-width
                                       (-> (+ start-line (count lines) -1) str count))))
         callout-indent     (when line-numbering?
                              (repeat (nchars (inc line-number-width') " ")))]
     (loop [[line-data & more-lines] lines
            line-number start-line
            result      []]
       (if-not line-data
         result
         (let [{:keys [line annotations]} line-data
               callout-lines (when (seq annotations)
                               (callouts style annotations))
               result'       (cond-> (conj result
                                           (list
                                             (when line-numbering?
                                               (list
                                                 [{:width line-number-width'}
                                                  line-number ":"]
                                                 " "))
                                             line))
                                     callout-lines (into
                                                     (map list callout-indent callout-lines)))]
           (recur more-lines (inc line-number) result')))))))

(defn underline-marker
  "Marker function that renders as a heavy underline."
  {:added "3.5.0"}
  [^long length]
  (str "┯" (nchars (dec length) "━")))


(defn create-annotation-set
  "Creates an annotation set from a seq of lines.

  Options:

  :width - number (default: 120). Total width available for line numbers, callouts, and annotation text.
    Annotation text will be soft-wrapped to fit within the available space.
  :window - number (default: 2) of lines before and after an annotated line to display (for context)
  :line-offset - number (default 1) adjustment to actual line number when rendering
  "
  {:since "3.10.0"}
  ([lines]
   (create-annotation-set lines nil))
  ([lines opts]
   {:lines    (vec lines)
    :opts     (merge {:width       120
                      :window      2
                      :line-offset 1
                      :style       *default-style*} opts)
    :callouts {}}))

(defn add-callout
  "Adds a callout to an annotation set. Options are the same as for [[callouts]],
  (:offset, :length, :font, :marker) though a :message key is added to the options."
  {:since "3.10.0"}
  [annotation-set line callout-opts & message]
  (update-in annotation-set [:callouts line] conj
             (assoc callout-opts :message message)))

(defn render-annotation-set
  "Invoked once all callouts have been added to the annotation set.
  Returns a seq of composed strings, each a single rendered line."
  {:since "3.10.0"}
  [annotation-set]
  (let [{:keys        [lines opts]
         set-callouts :callouts} annotation-set
        {:keys [width reroute-width window line-offset style]
         :or   {width Long/MAX_VALUE}} opts
        ;; Compute 0-based indexes of lines to be displayed; some of those lines
        ;; will include annotations.
        display-lines     (->> set-callouts
                               keys
                               (mapcat #(range (- % window) (+ % window 1)))
                               (remove neg?)
                               (remove #(>= % (count lines)))
                               distinct
                               sort)
        line-number-width (->> display-lines
                               last
                               (+ line-offset)
                               str
                               count
                               inc)                         ; Include the ":"
        callout-indent    (nchars (inc line-number-width) \space)
        callout-width     (- width line-number-width 1)]    ; include the space after the colon
    (reduce (fn [result linex]
              ;; Render each line; render callouts for each line that has callouts.
              (let [result' (conj result
                                  (list
                                    [{:width line-number-width} (+ linex line-offset) ":"]
                                    " "
                                    (nth lines linex)))]
                (if-let [line-callouts (get set-callouts linex)]
                  (->> (callouts
                         style
                         line-callouts
                         (cond-> {:width callout-width}
                                 reroute-width (assoc :reroute-width reroute-width)))
                       (map #(list callout-indent %))
                       (into result'))
                  result')))
            []
            display-lines)))
(ns clj-commons.ansi
  "Help with generating textual output that includes ANSI escape codes for formatting.
  The [[compose]] function is the best starting point.

  Specs for types and functions are in the [[spec]] namespace.

  Reference: [ANSI Escape Codes @ Wikipedia](https://en.wikipedia.org/wiki/ANSI_escape_code#SGR)."
  (:require [clojure.string :as str]
            [clj-commons.pretty-impl :refer [csi padding]]))

(defn- is-ns-loaded?
  [sym]
  (some? (find-ns sym)))

(defn- to-boolean
  [s]
  (-> s str/trim str/lower-case (= "true")))

(def ^:dynamic *color-enabled*
  "Determines if ANSI colors are enabled; color is a deliberate misnomer, as we lump
  other font characteristics (bold, underline, italic, etc.) along with colors.

  This will be false if the environment variable NO_COLOR is non-blank.

  Otherwise, the JVM system property `clj-commons.ansi.enabled` (if present) determines
  the value; \"true\" enables colors, any other value disables colors.

  If the property is null, then the default is a best guess based on the environment:
  if either the `nrepl.core` namespace is present, or the JVM has a console  (via `(System/console)`),
  then color will be enabled.

  The nrepl.core check has been verified to work with Cursive, with `lein repl`, and with `clojure` (or `clj`)."
  (if (seq (System/getenv "NO_COLOR"))
    false
    (let [flag (System/getProperty "clj-commons.ansi.enabled")]
      (cond
        (some? flag) (to-boolean flag)

        (is-ns-loaded? 'nrepl.core)
        true

        :else
        (some? (System/console))))))

(defmacro when-color-enabled
  "Evaluates its body only when [[*color-enabled*]] is true."
  [& body]
  `(when *color-enabled* ~@body))

;; select graphic rendition
(def ^:const ^:private sgr
  "The Select Graphic Rendition suffix: m"
  "m")

(def ^:const ^:private reset-font
  "ANSI escape code to resets all font characteristics."
  (str csi sgr))

(def ^:private font-terms
  ;; Map a keyword to a tuple of characteristic and SGR parameter value.
  ;; We track the current value for each characteristic.
  (reduce merge
          {:bold [:bold "1"]
           :plain [:bold "22"]
           :faint [:bold "2"]

           :italic [:italic "3"]
           :roman [:italic "23"]

           :inverse [:inverse "7"]
           :normal [:inverse "27"]

           :underlined [:underlined "4"]
           :not-underlined [:underlined "24"]}
          (map-indexed
            (fn [index color-name]
              {(keyword color-name) [:foreground (str (+ 30 index))]
               (keyword (str "bright-" color-name)) [:foreground (str (+ 90 index))]
               (keyword (str color-name "-bg")) [:background (str (+ 40 index))]
               (keyword (str "bright-" color-name "-bg")) [:background (str (+ 100 index))]})
            ["black" "red" "green" "yellow" "blue" "magenta" "cyan" "white"])))

(defn- compose-font
  "Uses values in current to build a font string that will reset all fonts characteristics then,
  as necessary, add back needed font characteristics."
  ^String [current]
  (when-color-enabled
    (let [codes (keep #(get current %) [:foreground :background :bold :italic :inverse :underlined])]
      (if (seq codes)
        (str csi "0;" (str/join ";" codes) sgr)
        ;; there were active characteristics, but current has none, so just reset font characteristics
        reset-font))))

(defn- split-font-def*
  [font-def]
  (assert (simple-keyword? font-def) "expected a simple keyword to define the font characteristics")
  (mapv keyword (str/split (name font-def) #"\.")))

(def ^:private memoized-split-font-def* (memoize split-font-def*))

(defn ^:private split-font-def
  [font-def]
  (if (vector? font-def)
    font-def
    (memoized-split-font-def* font-def)))

(defn- update-font-data-from-font-def
  [font-data font-def]
  (if (some? font-def)
    (let [ks (split-font-def font-def)
          f (fn [font-data term]
              ;; nils are allowed in the vector form of a font def
              (if (nil? term)
                font-data
                (let [[font-k font-value] (or (get font-terms term)
                                              (throw (ex-info (str "unexpected font term: " term)
                                                              {:font-term term
                                                               :font-def font-def
                                                               :available-terms (->> font-terms keys sort vec)})))]
                  (assoc! font-data font-k font-value))))]
      (persistent! (reduce f (transient font-data) ks)))
    font-data))

(defn- extract-span-decl
  [value]
  (cond
    (nil? value)
    nil

    ;; It would be tempting to split the keyword here, but that would obscure an error if the keyword
    ;; contains a substring that isn't an expected font term.
    (or (keyword? value)
        (vector? value))
    {:font value}

    (map? value)
    value

    :else
    (throw (ex-info "invalid span declaration"
                    {:font-decl value}))))

(defn- nil-or-empty-string?
  "True if an empty string, or nil; false otherwise, such as for numbers, etc."
  [value]
  (or (nil? value)
      (= "" value)))

(declare ^:private normalize-markup)

(defn- half-of
  [^long x round-up?]
  (let [base (Math/floorDiv x (long 2))]
    (+ base
       (if round-up?
         (mod x 2)
         0))))

(defn- apply-padding
  [terms pad width actual-width]
  (let [padding-needed (- width actual-width)
        left-padding (case pad
                       (:left nil) padding-needed
                       :both (half-of padding-needed true)
                       0)
        right-padding (case pad
                        :right padding-needed
                        :both (half-of padding-needed false)
                        0)
        left-padded (if (pos? left-padding)
                      (into [(first terms)
                             (padding left-padding)]
                            (next terms))
                      terms)]
    (cond-> left-padded
            (pos? right-padding)
            (conj (padding right-padding)))))

(defn- normalize-and-pad-markup
  "Given a span broken into decl and inputs, returns a map of :width (actual width, which may exceed
   requested width) and :span (a replacement span vector with added spaces)."
  [span-decl remaining-inputs]
  (let [{:keys [width pad]} span-decl
        ;; Transform this span and everything below it into easily managed span vectors, starting
        ;; with a version of this span decl.
        span-decl' (dissoc span-decl :width :pad)
        *width (volatile! 0)
        inputs' (into [span-decl'] (normalize-markup remaining-inputs *width))
        actual-width @*width]
    ;; If at or over desired width, don't need to pad
    (if (<= width actual-width)
      {:width actual-width
       :span inputs'}
      ;; Add the padding in the desired position(s); this ensures that the logic that generates
      ;; ANSI escape codes occurs correctly, with the added spaces getting the font for this span.
      {:width width
       :span (apply-padding inputs' pad width actual-width)})))


(defn- normalize-markup
  "Normalizes markup to span vectors, while keeping track of the total length of string values."
  [coll *width]
  (let [f (fn reducer [result input]
            (cond
              (nil-or-empty-string? input)
              result

              (vector? input)
              (let [decl (extract-span-decl (first input))
                    more-inputs (next input)
                    span (if (:width decl)
                           (let [{:keys [width span]} (normalize-and-pad-markup decl more-inputs)]
                             (vswap! *width + width)
                             span)
                           ;; Normalize contents while also tracking the width
                           (reduce reducer [decl] more-inputs))]
                (conj result span))

              (sequential? input)
              ;; Convert to a span with a nil decl
              (let [sub-span (reduce reducer [nil] input)]
                (conj result sub-span))

              :else
              (let [value-str ^String (str input)]
                (vswap! *width + (.length value-str))
                (conj result value-str))))]
    (reduce f [] coll)))

(defn- collect-markup
  [state input]
  (cond
    (nil-or-empty-string? input)
    state

    (vector? input)
    (let [[first-element & inputs] input
          {:keys [width font] :as span-decl} (extract-span-decl first-element)]
      (if width
        (recur state (:span (normalize-and-pad-markup span-decl inputs)))
        ;; Normal (no width tracking)
        (let [{:keys [current]} state]
          (-> (reduce collect-markup
                      (update state :current update-font-data-from-font-def font)
                      inputs)
              ;; At the end of the vector, return current (but not the active)
              ;; to what it was previously.  We leave active alone until we're about
              ;; to output.
              (assoc :current current)))))

    ;; Lists, lazy-lists, etc: processed recursively
    (sequential? input)
    (reduce collect-markup state input)

    :else
    (let [{:keys [active current ^StringBuilder buffer]} state
          state' (if (= active current)
                   state
                   (let [font-str (compose-font current)]
                     (when font-str
                       (.append buffer font-str))
                     (cond-> (assoc state :active current)
                             ;; Signal that a reset is needed at the very end
                             font-str
                             (assoc :dirty? (not= font-str reset-font)))))]
      (.append buffer (str input))
      state')))

(defn- compose*
  [inputs]
  (let [buffer (StringBuilder. 100)
        {:keys [dirty?]} (collect-markup {:active {}
                                          :current {}
                                          :buffer buffer}
                                         inputs)]
    (when dirty?
      (.append buffer reset-font))
    (.toString buffer)))

(defn compose
  "Given a Hiccup-inspired data structure, composes and returns a string that includes ANSI formatting codes
  for font color and other characteristics.

  The data structure may consist of literal values (strings, numbers, etc.) that are formatted
  with `str` and concatenated.

  Nested sequences are composed recursively; this (for example) allows the output from
  `map` or `for` to be mixed into the composed string seamlessly.

  Nested vectors represent _spans_, a sequence of values with a specific visual representation.
  The first element in a span vector declares the visual properties of the span: the font color
  and other font characteristics, and the width and padding (described later).
  Spans may be nested.

  The declaration is usually a keyword, to define just the font.
  The font def contains one or more terms, separated by periods.

  The terms:

  Characteristic   | Values
  ---              |---
  foreground color | `red` or `bright-red` (for each color)
  background color |  same as foreground color, with a `-bg` suffix (e.g., `red-bg`)
  boldness         | `bold`, `faint`, or `plain`
  italics          | `italic` or `roman`
  inverse          | `inverse` or `normal`
  underline        | `underlined` or `not-underlined`

  e.g.

  ```
  (compose [:yellow \"Warning: the \" [:bold.bright-white.bright-red-bg \"reactor\"]
    \" is about to \"
    [:italic.bold.red \"meltdown!\"]])
  => ...
  ```

  The order of the terms does not matter. Behavior for conflicting terms (e.g., `:blue.green.black`)
  is not defined.

  Font defs apply on top of the font def of the enclosing span, and the outer span's font def
  is restored at the end of the inner span, e.g. `[:red \" RED \" [:bold \"RED/BOLD\"] \" RED \"]`.

  Alternately, a font def may be a vector of individual keywords, e.g., `[[:bold :red] ...]` rather than
  `[:bold.red ...]`.  This works better when the exact font characteristics are determined
  dynamically.

  A font def may also be nil, to indicate no change in font.

  `compose` presumes that on entry the current font is plain (default foreground and background, not bold,
  or inverse, or italic, or underlined) and appends a reset sequence to the end of the returned string to
  ensure that later output is also plain.

  The core colors are `black`, `red`, `green`, `yellow`, `blue`, `magenta`, `cyan`, and `white`.

  When [[*color-enabled*]] is false, then any font defs are validated, but otherwise ignored (no ANSI codes
  will be included in the composed string).

  The span's font declaration may also be a map with the following keys:

  Key    | Type                          | Description
  ---    |---                            |---
  :font  | keyword or vector of keywords | The font declaration
  :width | number                        | The desired width of the span
  :pad   | :left, :right, :both          | Where to pad the span if :width specified, default is :left

  The map form of the font declaration is typically only used when a span width is specified.
  The span will be padded with spaces to ensure that it is the specified width.  `compose` tracks the number
  of characters inside the span, excluding any ANSI code sequences injected by `compose`.

  Padding adds spaces; thus aligning the text on the left means padding on the right, and vice-versa.

  Setting the padding to :both will add spaces to both the left and the right; the content will be centered.
  If the necessary amount of padding is odd, the extra space will appear on the left.

  `compose` doesn't consider the characters when calculating widths;
  if the strings contain tabs, newlines, or ANSI code sequences not generated by `compose`,
  the calculation of the span width will be incorrect.

  Example:

      [{:font :red
        :width 20} message]

  This will output the value of `message` in red text, padded with spaces on the left to be 20 characters.

  `compose` does not truncate a span to a width, it only pads if the span in too short."
  {:added "1.4.0"}
  [& inputs]
  (compose* inputs))

(defn pout
  "Composes its inputs as with [[compose]] and then prints the results, with a newline."
  {:added "3.2"}
  [& inputs]
  (println (compose* inputs)))

(defn pcompose
  "Composes its inputs as with [[compose]] and then prints the results, with a newline.

  Deprecated: use [[pout]] instead."
  {:added "2.2"
   :deprecated "3.2.0"}
  [& inputs]
  (println (compose* inputs)))

(defn perr
  "Composes its inputs as with [[compose]] and then prints the result with a newline to `*err*`."
  {:added "2.3.0"}
  [& inputs]
  (binding [*out* *err*]
    (println (compose* inputs))))



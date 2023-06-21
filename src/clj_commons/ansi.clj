(ns clj-commons.ansi
  "Help with generating textual output that includes ANSI escape codes for formatting.
  The [[compose]] function is the best starting point.

  Reference: [Wikipedia](https://en.wikipedia.org/wiki/ANSI_escape_code#SGR)."
  (:require [clojure.string :as str]
            [clj-commons.pretty-impl :refer [csi padding]]))

(defn- is-ns-available? [sym]
  (try
    (require sym)
    true
    (catch Throwable _ false)))

(defn- to-boolean
  [s]
  (when s
    (= "true" (-> s str/trim str/lower-case))))

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
    (let [flag (to-boolean (System/getProperty "clj-commons.ansi.enabled"))]
      (cond
        flag flag

        (is-ns-available? 'nrepl.core)
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

(defn- delta
  [active current k]
  (let [current-value (get current k)]
    (when (not= (get active k) current-value)
      current-value)))

(defn- compose-font
  ^String [active current]
  (when-color-enabled
    (let [codes (keep #(delta active current %) [:foreground :background :bold :italic :inverse :underlined])]
      (when (seq codes)
        (str csi (str/join ";" codes) sgr)))))

(defn- split-font-def*
  [font-def]
  (assert (simple-keyword? font-def) "expected a simple keyword to define the font characteristics")
  (mapv keyword (str/split (name font-def) #"\.")))

(def ^:private split-font-def (memoize split-font-def*))

(defn- update-font-data-from-font-def
  [font-data font-def]
  (if (some? font-def)
    (let [ks (split-font-def font-def)
          f (fn [font-data term]
              (let [[font-k font-value] (or (get font-terms term)
                                            (throw (ex-info (str "unexpected font term: " term)
                                                            {:font-term term
                                                             :font-def font-def
                                                             :available-terms (->> font-terms keys sort vec)})))]
                (assoc! font-data font-k font-value)))]
      (persistent! (reduce f (transient font-data) ks)))
    font-data))

(defn- collect-markup
  [state input]
  (cond
    (or
      (nil? input)
      (= "" input))
    state

    (vector? input)
    (let [[font-def & inputs] input
          {:ansi/keys [width pad]} (meta input)
          {:keys [current *width tracking-width? buffer]} state
          _ (when (and width tracking-width?)
              (throw (ex-info "can only track one column width at a time"
                              {:input input})))
          start-width @*width
          start-length (.length buffer)                     ; Needed if :pad is :left
          state' (reduce collect-markup
                   (-> state
                       (cond-> width (assoc :tracking-width? true))
                     (update :current update-font-data-from-font-def font-def)
                     (update :stack conj current))
                   inputs)]
      ;; TODO: treat the spaces same as other characters and deal with deferred
      ;; font characteristic changes?  This will be visible with inverse or
      ;; underlined spans.
      (when width
        (let [actual-width (- @*width start-width)
              spaces (padding (- width actual-width))]
         #_(prn {:start-width start-width
                :current-width @*width
                :width width
                :actual-width actual-width
                :buffer (str buffer)
                :start-length start-length
                :pad pad})
         (when spaces
           (if (= :right pad)
             (.append buffer spaces)
             (.insert buffer start-length spaces))
           ;; Not really necessary since we don't/can't track nested widths
           (vswap! *width + (.length spaces)))))
      (-> state'
        (assoc :current current
               :tracking-width? false)
        (update :stack pop)))

    ;; Lists, lazy-lists, etc: processed recursively
    (sequential? input)
    (reduce collect-markup state input)

    :else
    (let [{:keys [active current ^StringBuilder buffer *width]} state
          state' (if (= active current)
                   state
                   (let [font-str (compose-font active current)]
                     (when font-str
                       ;; This never counts towards *width
                       (.append buffer font-str))
                     (cond-> (assoc state :active current)
                             font-str (assoc :dirty? true))))
          input-str (str input)]
      (.append buffer input-str)
      (vswap! *width + (.length input-str))
      state')))

(defn compose
  "Given a Hiccup-inspired data structure, composes and returns a string that includes necessary ANSI codes.

  The data structure may consist of literal values (strings, numbers, etc.) that are formatted
  with `str` and concatenated.

  Nested sequences are composed recursively; this (for example) allows the output from
  `map` or `for` to be mixed into the composed string seamlessly.

  Nested vectors represent blocks; the first element in the vector is a keyword
  that defines the font used within the block.  The font def contains one or more terms,
  separated by periods.

  The terms:

  - foreground color:  e.g. `red` or `bright-red`
  - background color: e.g., `green-bg` or `bright-green-bg`
  - boldness: `bold`, `faint`, or `plain`
  - italics: `italic` or `roman`
  - inverse: `inverse` or `normal`
  - underline: `underlined` or `not-underlined`

  e.g.

  ```
  (compose [:yellow \"Warning: the \" [:bold.bright-white.bright-red-bg \"reactor\"]
    \" is about to \"
    [:italic.bold.red \"meltdown!\"]])
  => ...
  ```

  The order of the terms does not matter. Behavior for conflicting terms (`:blue.green.black`)
  is not defined.

  Font defs apply on top of the font def of the enclosing block, and the outer block's font def
  is restored at the end of the inner block, e.g. `[:red \" RED \" [:bold \"RED/BOLD\"] \" RED \"]`.

  A font def may also be nil, to indicate no change in font.

  `compose` presumes that on entry the current font is plain (default foreground and background, not bold,
   or inverse, or italic, or underlined) and appends a reset sequence to the end of the returned string to
   ensure that later output is also plain.

  The core colors are `black`, `red`, `green`, `yellow`, `blue`, `magenta`, `cyan`, and `white`.

  When [[*color-enabled*]] is false, then any font defs are validated, then ignored (no ANSI codes
  will be included).

  Vectors may also have meta-data to control columns width and padding.
  - ^:ansi/width - the width of the column
  - ^:ansi/pad - placement of padding spaces :left or :right, defaults to :left

  Only one column may be active at a time (a vector with a width may not contain another vector with a width).
  `compose` will insert extra spaces before of after the content of the vector (however, if the column's actual width
  exceeds the width specified, nothing is truncated).  The width is calculated based on visible text - the
  ANSI codes inserted by `compose` do not count.

  Example:
      ^{:ansi/width 20}[:red message]

  This will output the `message`, padded on the left to be 20 characters.

  At this time, the placement of the spaces may be a bit haphazard with respect to ANSI codes; the spaces
  may be visible if the font def sets inverse, underlined, or colored backgrounds."
  {:added "1.4.0"}
  [& inputs]
  (let [initial-font {:foreground "39"
                      :background "49"
                      :bold "22"
                      :italic "23"
                      :inverse "27"
                      :underlined "24"}
        buffer (StringBuilder. 100)
        {:keys [dirty?]} (collect-markup {:stack []
                                          :active initial-font
                                          :current initial-font
                                          :buffer buffer
                                          :*width (volatile! 0)}
                                         inputs)]
    (when dirty?
      (.append buffer reset-font))
    (.toString buffer)))


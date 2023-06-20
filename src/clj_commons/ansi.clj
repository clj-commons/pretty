(ns clj-commons.ansi
  "Help with generating textual output that includes ANSI escape codes for formatting.
  The [[compose]] function is the best starting point.

  Reference: [Wikipedia](https://en.wikipedia.org/wiki/ANSI_escape_code#SGR)."
  (:require [clojure.string :as str]))

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

  The JVM system property `clj-commons.ansi.enabled` (if present) determines
  the value; \"true\" enables colors, any other value disables colors.

  If the property is null, then the default is a best guess based on the environment:
  if either the nrepl.core namespace is present, or the JVM has a console  (via `(System/console)`),
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
  [& body]
  `(when *color-enabled* ~@body))

(def ^:const csi
  "The control sequence initiator: `ESC [`"
  "\u001b[")

;; select graphic rendition
(def ^:const sgr
  "The Select Graphic Rendition suffix: m"
  "m")

(def ^:const reset-font
  "Resets all font characteristics."
  (str csi sgr))

(def ^:const ^:private ansi-pattern #"\e\[.*?m")

(defn strip-ansi
  "Removes ANSI font characteristic codes from a string, returning just the plain text."
  ^String [string]
  (str/replace string ansi-pattern ""))

(defn visual-length
  "Returns the length of the string, with ANSI codes stripped out."
  [string]
  (-> string strip-ansi .length))

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

(defn- delta [active current k]
  (let [current-value (get current k)]
    (when (not= (get active k) current-value)
      current-value)))

(defn- compose-font
  [active current]
  (let [codes (keep #(delta active current %) [:foreground :background :bold :italic :inverse :underlined])]
    (when (and *color-enabled* (seq codes))
      (str csi (str/join ";" codes) sgr))))

(defn- parse-font-def
  [font-data font-def]
  (when (some? font-def)
    (assert (simple-keyword? font-def) "expected a simple keyword to define the font characteristics")
    (let [ks (str/split (name font-def) #"\.")
          f (fn [font-data term]
              (let [[font-k font-value] (or (get font-terms term)
                                            (throw (ex-info (str "unexpected font term: " term)
                                                            {:font-term term
                                                             :font-def font-def
                                                             :available-terms (->> font-terms keys sort vec)})))]
                (assoc font-data font-k font-value)))]
      (reduce f font-data (map keyword ks)))))

(defn- collect-markup
  [state input]
  (cond
    (or
      (nil? input)
      (= "" input))
    state

    (vector? input)
    (let [[font-def & inputs] input
          {:keys [current]} state
          state' (reduce collect-markup
                   (-> state
                     (assoc :current (parse-font-def current font-def))
                     (update :stack conj current))
                   inputs)]
      (-> state'
        (assoc :current current)
        (update :stack pop)))

    ;; Lists, lazy-lists, etc: processed recursively
    (sequential? input)
    (reduce collect-markup state input)

    :else
    (let [{:keys [active current]} state
          state' (if (= active current)
                   state
                   (let [font-str (compose-font active current)]
                     (cond-> (assoc state :active current)
                             font-str (-> (update :results conj font-str)
                                          (assoc :dirty? true)))))]
      (update state' :results conj (str input)))))

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
   or inverse, or italic) and appends a [[reset-font]] to the end of the returned string to
   ensure that later output is also plain.

  The core colors are `black`, `red`, `green`, `yellow`, `blue`, `magenta`, `cyan`, and `white`.

  When `*color-enabled*` is false, the result is just the concatenation of all the values, skipping nils
  and ignoring font defs.
  "
  {:added "1.4.0"}
  [& input]
  ;; The initial font is used so that after a top-level font change, there's something to
  ;; change back to.
  (let [initial-font {:foreground "39"
                      :background "49"
                      :bold "22"
                      :italic "23"
                      :inverse "27"
                      :underlined "24"}
        {:keys [results dirty?]} (collect-markup {:stack ()
                                                  :active initial-font
                                                  :current initial-font
                                                  :results []}
                                                 input)
        sb (StringBuilder. 100)]
    (doseq [s results
            :when (some? s)]
      (.append sb ^String s))
    (when dirty?
      (.append sb reset-font))
    (.toString sb)))

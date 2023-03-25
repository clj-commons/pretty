(ns io.aviso.ansi
  "Help with generating textual output that includes ANSI escape codes for formatting.
  The [[compose]] function is the best starting point.

  Reference: [Wikipedia](https://en.wikipedia.org/wiki/ANSI_escape_code#SGR)"
  (:require [clojure.string :as str]))

(defn- is-ns-available? [sym]
  (try
    (require sym)
    true
    (catch Throwable _ false)))

(def ^:const ^{:added "1.3"} ansi-output-enabled?
  "Determine if ANSI output is enabled.

  The first checks are for the JVM system property `io.aviso.ansi.enable`
  and then system property ENABLE_ANSI_COLORS.
  If either of these is non-nil, it sets the value:
  the value `false` (matched caselessly) disables ANSI colors and fonts,
  otherwise they are enabled.

  Next, there is an attempt to determine if execution is currently inside a REPL environment,
  possibly started from an IDE; a check is made to see if `nrepl.core` namespace is available;
  if so, then ANSI colors are enabled.

  This has been verified to work with Cursive, with `lein repl`, and with `clojure` (or `clj`).

  This check is necessary, because often in such cases, there is no console (the next check).

  Otherwise, if the system has a console (via `(System/console)`) ANSI output will be enabled;
  when Clojure is running in a pipe, or as a background job without a terminal attached, the console
  will be nil and ANSI output will be disabled.

  When this value is false, all the generated color and font constants return the empty string, and the
  color and font functions return the input string unchanged.  This is decided during macro expansion when
  the ansi namespace is first loaded, so it can't be changed at runtime."
  (if-let [value (or
                   (System/getProperty "io.aviso.ansi.enable")
                   (System/getenv "ENABLE_ANSI_COLORS"))]
    (not (.equalsIgnoreCase value "false"))
    (some?
      (or
        (is-ns-available? 'nrepl.core)
        (System/console)))))

(defmacro ^:private if-enabled?
  [expr]
  (if ansi-output-enabled?
    expr
    ""))

(def ^:const csi
  "The control sequence initiator: `ESC [`"
  "\u001b[")

;; select graphic rendition
(def ^:const sgr
  "The Select Graphic Rendition suffix: m"

  "m")

(def ^:const reset-font
  "Resets the font, clearing bold, italic, color, and background color."
  (if-enabled? (str csi sgr)))

(defmacro ^:private def-sgr-const
  "Utility for defining a font-modifying constant."
  [symbol-name color-name & codes]
  `(def ~(vary-meta (symbol symbol-name) assoc :const true)
     ~(format "Constant for ANSI code to enable %s text." color-name)
     ~(if-enabled? (str csi (str/join ";" codes) sgr))))

(defmacro ^:private def-sgr-fn
  "Utility for creating a function that enables some combination of SGR codes around some text, but resets
  the font after the text."
  [fn-name color-name & codes]
  (let [arg 'text
        prefix (str csi (str/join ";" codes) sgr)]
    `(defn ~(symbol fn-name)
       ~(format "Wraps the provided text with ANSI codes to render as %s text." color-name)
       [~arg]
       ~(if ansi-output-enabled?
          `(str ~prefix ~arg ~reset-font)
          arg))))

;;; Define functions and constants for each color. The functions accept a string
;;; and wrap it with the ANSI codes to set up a rendition before the text,
;;; and reset the rendition fully back to normal after.
;;; The constants enable the rendition, and require the reset-font value to
;;; return to normal.
;;; For each color C:
;;; - functions:
;;;   - C: change text to that color (e.g., "green")
;;;   - C-bg: change background to that color (e.g., "green-bg")
;;;   - bold-C: change text to bold variation of color (e.g., "bold-green")
;;;   - bold-C-bg: change background to bold variation of color (e.g., "bold-green-bg")
;;; - constants
;;;   - C-font: enable text in that color (e.g., "green-font")
;;;   - C-bg-font: enable background in that color (e.g., "green-bg-font")
;;;   - bold-C-font; enable bold text in that color (e.g., "bold-green-font")
;;;   - bold-C-bg-font; enable background in that bold color (e.g., "bold-green-bg-font")

(defmacro ^:private define-colors
  []
  `(do
     ~@(map-indexed
         (fn [index color-name]
           `(do
              (def-sgr-fn ~color-name ~color-name ~(+ 30 index))
              (def-sgr-fn ~(str color-name "-bg") ~(str color-name " background") ~(+ 40 index))
              (def-sgr-fn ~(str "bold-" color-name) ~(str "bold " color-name) 1 ~(+ 30 index))
              (def-sgr-fn ~(str "bold-" color-name "-bg") ~(str "bold " color-name " background") 1 ~(+ 40 index))
              (def-sgr-const ~(str color-name "-font") ~color-name ~(+ 30 index))
              (def-sgr-const ~(str color-name "-bg-font") ~(str color-name " background") ~(+ 40 index))
              (def-sgr-const ~(str "bold-" color-name "-font") ~(str "bold " color-name) 1 ~(+ 30 index))
              (def-sgr-const ~(str "bold-" color-name "-bg-font") ~(str "bold " color-name " background") 1 ~(+ 40 index))))
         ["black" "red" "green" "yellow" "blue" "magenta" "cyan" "white"])))

(define-colors)

;; ANSI defines quite a few more, but we're limiting to those that display properly in the
;; Cursive REPL.

(defmacro ^:private define-fonts
  []
  `(do
     ~@(for [[font-name code] [['bold 1]
                               ['plain 22]
                               ['italic 3]
                               ['roman 23]
                               ['inverse 7]
                               ['normal 27]
                               ['default-foreground 39]
                               ['default-background 49]]]
         `(do
            (def-sgr-fn ~font-name ~font-name ~code)
            (def-sgr-const ~(str font-name "-font") ~font-name ~code)))))

(define-fonts)

(def ^:const ^:private ansi-pattern #"\e\[.*?m")

(defn ^String strip-ansi
  "Removes ANSI codes from a string, returning just the raw text."
  [string]
  (str/replace string ansi-pattern ""))

(defn visual-length
  "Returns the length of the string, with ANSI codes stripped out."
  [string]
  (-> string strip-ansi .length))

(def ^:private
  font-terms
  {:black [:foreground black-font]
   :bold-black [:foreground bold-black-font]
   :black-bg [:background black-bg-font]
   :bold-black-gb [:background bold-black-bg-font]

   :red [:foreground red-font]
   :bold-red [:foreground bold-red-font]
   :red-bg [:background red-bg-font]
   :bold-red-gb [:background bold-red-bg-font]


   :green [:foreground green-font]
   :bold-green [:foreground bold-green-font]
   :green-bg [:background green-bg-font]
   :bold-green-gb [:background bold-green-bg-font]

   :yellow [:foreground yellow-font]
   :bold-yellow [:foreground bold-yellow-font]
   :yellow-bg [:background yellow-bg-font]
   :bold-yellow-gb [:background bold-yellow-bg-font]

   :blue [:foreground blue-font]
   :bold-blue [:foreground bold-blue-font]
   :blue-bg [:background blue-bg-font]
   :bold-blue-gb [:background bold-blue-bg-font]

   :magenta [:foreground magenta-font]
   :bold-magenta [:foreground bold-magenta-font]
   :magenta-bg [:background magenta-bg-font]
   :bold-magenta-gb [:background bold-magenta-bg-font]

   :cyan [:foreground cyan-font]
   :bold-cyan [:foreground bold-cyan-font]
   :cyan-bg [:background cyan-bg-font]
   :bold-cyan-gb [:background bold-cyan-bg-font]

   :white [:foreground white-font]
   :bold-white [:foreground bold-white-font]
   :white-bg [:background white-bg-font]
   :bold-white-gb [:background bold-white-bg-font]

   :bold [:bold bold-font]
   :plain [:bold plain-font]

   :italic [:italic italic-font]
   :roman [:italic roman-font]

   :inverse [:inverse inverse-font]
   :normal [:inverse normal-font]})

(defn- delta [active current k]
  (let [current-value (get current k)]
    (when (not= (get active k) current-value)
      current-value)))

(defn- compose-font
  [active current]
  (reduce str (keep #(delta active current %) [:foreground :background :bold :italic :inverse])))

(defn- parse-font
  [font-data font-def]
  {:pre [(keyword? font-def)]}
  (let [ks (str/split (name font-def) #"\.")
        f  (fn [font-data term]
             (let [[font-k font-value] (or (get font-terms term)
                                           (throw (ex-info (str "Unexpected font term: " term)
                                                    {:font-term term
                                                     :font-def font-def
                                                     :available-terms (->> font-terms keys sort vec)})))]
               (assoc font-data font-k font-value)))]
    (reduce f font-data (map keyword ks))))

(defn- collect-markup
  [state input]
  (cond
    (nil? input)
    state

    (vector? input)
    (let [[font-def & inputs] input
          {:keys [current]} state
          state' (reduce collect-markup
                   (-> state
                     (assoc :current (parse-font current font-def))
                     (update :stack conj current))
                   inputs)]
      (-> state'
        (assoc :current current)
        (update :stack pop)))

    ;; Lists, lazy-lists, etc: processed recusively
    (sequential? input)
    (reduce collect-markup state input)

    (and (string? input)
      (str/blank? input))
    state

    :else
    (let [{:keys [active current]} state
          state' (if (= active current)
                   state
                   (-> state
                     (update :results conj (compose-font active current))
                     (assoc :active current)))]
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

  - foreground color:  e.g. `red` or `bold-red`
  - background color: e.g., `green-bg` or `bold-green-bg`
  - boldness: `bold` or `plain`
  - italics: `italic` or `roman`
  - inverse: `inverse` or `normal`

  e.g.

  ```
  (compose [:yellow \"Warning: the \" [:bold.bold-white.bold-red-bg \"reactor\"]
    \" is about to \"
    [:italic.bold-red \"meltdown!\"]])
  => ...
  ```

  Font defs apply on top of the font def of the enclosing block, and the outer block's font def
  is restored at the end of the inner block, e.g. `[:red \" RED \" [:bold \"RED/BOLD\"] \" RED \"]`.


  `compose` presumes that on entry the current font is plain (default foreground and background, not bold,
   or inverse, or italic) and appends a [[reset-font]] to the end of the returned string to
   ensure that later output is also plain.
  "
  {:added "1.4.0"}
  [& input]

  (let [initial-font {:foreground default-foreground-font
                      :background default-background-font
                      :bold plain-font
                      :italic roman-font
                      :inverse normal-font}
        {:keys [results]} (collect-markup {:stack ()
                                           :active initial-font
                                           :current initial-font
                                           :results []}
                            input)
        sb           (StringBuilder. 100)]
    (doseq [s results]
      (.append sb ^String s))
    (.append sb reset-font)                                 ;; TODO: May not always be necessary
    (.toString sb)))


(ns io.aviso.ansi
  "Help with generating textual output that includes ANSI escape codes for formatting.
  The [[compose]] function is the best starting point.

  Reference: [Wikipedia](https://en.wikipedia.org/wiki/ANSI_escape_code#SGR).

  In version 1.4, the incorrectly named `bold-<color>` functions and constants
  were deprecated in favor of the `bright-<color>` equivalents (correcting
  a day 1 naming mistake)."
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
  "Resets the font, clearing all colors and other attributes."
  (if-enabled? (str csi sgr)))

(defmacro ^:private def-sgr-const
  "Utility for defining a font-modifying constant."
  [symbol-name meta desc & codes]
  `(def ~(cond-> (with-meta (symbol symbol-name) {:const true})
           meta (vary-meta merge meta))
     ~(format "Constant for ANSI code to render %s." desc)
     ~(if-enabled? (str csi (str/join ";" codes) sgr))))

(defmacro ^:private def-sgr-fn
  "Utility for creating a function that enables some combination of SGR codes around some text, but resets
  the font after the text."
  [fn-name meta desc & codes]
  (let [arg 'text
        prefix (str csi (str/join ";" codes) sgr)]
    `(defn ~(cond-> (symbol fn-name)
              meta (vary-meta merge meta))
       ~(format "Wraps the provided text with ANSI codes to render %s." desc)
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
              (def-sgr-fn ~color-name nil ~(str "text in " color-name) ~(+ 30 index))
              (def-sgr-fn ~(str color-name "-bg") nil ~(str "with a " color-name " background") ~(+ 40 index))
              (def-sgr-fn ~(str "bold-" color-name) {:deprecated "1.4"} ~(str "text in bright " color-name) 1 ~(+ 30 index))
              (def-sgr-fn ~(str "bold-" color-name "-bg") {:deprecated "1.4"} ~(str "with a bright " color-name " background") 1 ~(+ 40 index))
              (def-sgr-const ~(str color-name "-font") nil ~(str "text in " color-name) ~(+ 30 index))
              (def-sgr-const ~(str color-name "-bg-font") nil ~(str "with a " color-name " background") ~(+ 40 index))
              (def-sgr-const ~(str "bold-" color-name "-font") {:deprecated "1.4"} ~(str "text in bright " color-name) 1 ~(+ 30 index))
              (def-sgr-const ~(str "bold-" color-name "-bg-font") {:deprecated "1.4"} ~(str "with a bright " color-name " background") 1 ~(+ 40 index))
              (def-sgr-fn ~(str "bright-" color-name) nil ~(str "text in bright " color-name) 1 ~(+ 30 index))
              (def-sgr-fn ~(str "bright-" color-name "-bg") nil ~(str "with a bright " color-name " background") 1 ~(+ 40 index))
              (def-sgr-const ~(str "bright-" color-name "-font") nil ~(str "text in bright " color-name) 1 ~(+ 30 index))
              (def-sgr-const ~(str "bright-" color-name "-bg-font") nil ~(str "with a bright " color-name " background") 1 ~(+ 40 index))))
         ["black" "red" "green" "yellow" "blue" "magenta" "cyan" "white"])))

(define-colors)

;; ANSI defines quite a few more, but we're limiting to those that display properly in the
;; Cursive REPL.

(defmacro ^:private define-fonts
  []
  `(do
     ~@(for [[font-name desc code meta]
             [['bold "in bold" 1]
              ['plain "as plain (not bold or faint)" 22 {:added "1.4"}]
              ['faint "as faint (not bold or plain)" 2 {:added "1.4.1"}]
              ['italic "italicized" 3]
              ['roman "as romain (not italic)" 23 {:added "1.4"}]
              ['inverse "as inverse (foreground and background colors reversed)" 7]
              ['normal "as normal (not inverse)" 27 {:added "1.4"}]
              ['default-foreground "with the default foreground color" 39 {:added "1.4"}]
              ['default-background "with the default background color" 49 {:added "1.4"}]
              ['underlined "underlined" 4 {:added "1.4.1"}]
              ['not-underlined "not underlined" 24 {:added "1.4.1"}]]]
         `(do
            (def-sgr-fn ~font-name ~meta ~desc ~code)
            (def-sgr-const ~(str font-name "-font") ~meta ~desc ~code)))))

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
   :bright-black [:foreground bright-black-font]
   :black-bg [:background black-bg-font]
   :bright-black-gb [:background bright-black-bg-font]

   :red [:foreground red-font]
   :bright-red [:foreground bright-red-font]
   :red-bg [:background red-bg-font]
   :bright-red-gb [:background bright-red-bg-font]


   :green [:foreground green-font]
   :bright-green [:foreground bright-green-font]
   :green-bg [:background green-bg-font]
   :bright-green-gb [:background bright-green-bg-font]

   :yellow [:foreground yellow-font]
   :bright-yellow [:foreground bright-yellow-font]
   :yellow-bg [:background yellow-bg-font]
   :bright-yellow-gb [:background bright-yellow-bg-font]

   :blue [:foreground blue-font]
   :bright-blue [:foreground bright-blue-font]
   :blue-bg [:background blue-bg-font]
   :bright-blue-gb [:background bright-blue-bg-font]

   :magenta [:foreground magenta-font]
   :bright-magenta [:foreground bright-magenta-font]
   :magenta-bg [:background magenta-bg-font]
   :bright-magenta-gb [:background bright-magenta-bg-font]

   :cyan [:foreground cyan-font]
   :bright-cyan [:foreground bright-cyan-font]
   :cyan-bg [:background cyan-bg-font]
   :bright-cyan-gb [:background bright-cyan-bg-font]

   :white [:foreground white-font]
   :bright-white [:foreground bright-white-font]
   :white-bg [:background white-bg-font]
   :bright-white-gb [:background bright-white-bg-font]

   :bold [:bold bold-font]
   :plain [:bold plain-font]
   :faint [:bold faint-font]

   :italic [:italic italic-font]
   :roman [:italic roman-font]

   :inverse [:inverse inverse-font]
   :normal [:inverse normal-font]

   :underlined [:underlined underlined-font]
   :not-underlined [:underlined not-underlined-font]})

(defn- delta [active current k]
  (let [current-value (get current k)]
    (when (not= (get active k) current-value)
      current-value)))

(defn- compose-font
  [active current]
  (reduce str (keep #(delta active current %) [:foreground :background :bold :italic :inverse :underlined])))

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

    ;; Lists, lazy-lists, etc: processed recursively
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
                      :inverse normal-font
                      :underlined not-underlined-font}
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


(ns io.aviso.ansi
  "Help with generating textual output that includes ANSI escape codes for formatting."
  (:require
    [clojure.string :as str]
    [io.aviso.ansi-macros :as m])
  #?(:cljs
     (:require-macros
       [io.aviso.ansi-macros :as m])))

(def ^:const csi
  "The control sequence initiator: `ESC [`"
  "\u001b[")

;; select graphic rendition
(def ^:const sgr
  "The Select Graphic Rendition suffix: m"
  "m")

(def ^:const reset-font
  "Resets the font, clearing bold, italic, color, and background color."
  (str csi sgr))

(m/define-colors)
(m/define-fonts)

(def ^:const ^:private ansi-pattern #"\e\[.*?m")

(defn ^String strip-ansi
  "Removes ANSI codes from a string, returning just the raw text."
  [string]
  (str/replace string ansi-pattern ""))

(defn visual-length
  "Returns the length of the string, with ANSI codes stripped out."
  [string]
  (-> string strip-ansi .length))

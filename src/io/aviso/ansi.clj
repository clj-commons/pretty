(ns io.aviso.ansi
  "Help with generating textual output that includes ANSI escape codes for formatting."
  (:require [clojure.string :as str]))

;; control sequence initiator: ESC [
(def ^:const csi "\u001b[")

;; select graphic rendition
(def ^:const sgr "m")

(def ^:const ^{:doc "Resets the font, clearing bold, italic, color, and background color."}
  reset-font (str csi sgr))

(defn ^:private def-sgr-const
  "Utility for defining a font-modifying constant."
  [symbol-name code]
  (eval
    `(def ^:const ~(symbol symbol-name) ~(str csi code sgr))))

(defn ^:private def-sgr-fn
  "Utility for creating a function that enables some combination of SGR codes around some text, but resets
  the font after the text."
  [fn-name & codes]
  (eval
    `(defn ~(symbol fn-name)
       [text#]
       (str ~(str csi (str/join ";" codes) sgr) text# reset-font))))

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
;;;   - C-bg-font: enable backgroun in that bold color (e.g., "green-bg-font")
(doall
  (map-indexed (fn [index color-name]
                 (def-sgr-fn color-name (+ 30 index))
                 (def-sgr-fn (str color-name "-bg") (+ 40 index))
                 (def-sgr-fn (str "bold-" color-name) 1 (+ 30 index))
                 (def-sgr-fn (str "bold-" color-name "-bg") 1 (+ 40 index))
                 (def-sgr-const (str color-name "-font") (+ 30 index))
                 (def-sgr-const (str color-name "-bg-font") (+ 40 index)))
               ["black" "red" "green" "yellow" "blue" "magenta" "cyan" "white"]))

;; ANSI defines quite a few more, but we're limiting to those that display properly in the
;; Cursive REPL.

(def-sgr-fn "bold" 1)
(def-sgr-fn "italic" 3)

(def ^:const bold-font (str csi 1 sgr))
(def ^:const italic-font (str csi 3 sgr))
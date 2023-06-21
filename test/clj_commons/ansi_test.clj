(ns clj-commons.ansi-test
  (:require [clj-commons.ansi :as ansi]
            [clojure.string :as str]
            [clojure.test :refer [deftest is are]]
            [clj-commons.ansi :refer [compose *color-enabled*]]
            [clj-commons.pretty-impl :refer [csi]]))

(deftest sanity-check
  (is (= true *color-enabled*)))

(defn- safe-compose
  [input]
  (-> (apply compose input)
      (str/replace csi "[CSI]")))

(deftest nested-width-exception
  (when-let [e (is (thrown? Exception
                            (compose "START"
                                     ^{:ansi/width 10} [:red "A" "B"
                                                        ^{:ansi/width 20} [:blue "C"]])))]
    (is (= "can only track one column width at a time"
           (ex-message e)))
    (is (= {:input [:blue "C"]}
           (ex-data e)))))

(deftest compose-test
  (are [input expected]
    (= expected (safe-compose input))

    ;; For the moment, everything is suffixed with the [CSI]m reset.

    ["Simple"]
    "Simple"

    ["String" \space :keyword \space 'symbol \space 123 \space 44.5]
    "String :keyword symbol 123 44.5"

    ;; Handles nested lists

    ["Prefix--"

     (for [i (range 3)]
       (str " " i))

     " --Suffix"]
    "Prefix-- 0 1 2 --Suffix"

    ;; Check for skipping nils and blank strings, and not emitting the reset if no font
    ;; changes occurred.
    ["Prefix--"  [:bold nil ""] "--Suffix"]
    "Prefix----Suffix"

    ;; A bug caused blank strings to be omitted, this checks for the fix:
    [" "
     "|"
     "  "
     "|"
     "   "]
    " |  |   "

    ["Notice: the "
     [:yellow "shields"]
     " are operating at "
     [:green "98.7%"]
     "."]
    "Notice: the [CSI]33mshields[CSI]39m are operating at [CSI]32m98.7%[CSI]39m.[CSI]m"

    ;; nil is allowed (this is used when formatting is optional, such as the fonts in exceptions).

    ["NORMAL" [nil "-STILL NORMAL"]]
    "NORMAL-STILL NORMAL"


    ;; Demonstrate some optimizations (no change between first and second
    ;; INV/BOLD), and also specifying multiple font modifiers.

    ["NORMAL"
     [:red "-RED"]
     [:bright-red "-BR/RED"]]
    "NORMAL[CSI]31m-RED[CSI]91m-BR/RED[CSI]m"

    ["NORMAL-"
     [:inverse "-INVERSE" [:bold "-INV/BOLD"]]
     [:inverse.bold "-INV/BOLD"]
     "-NORMAL"]
    "NORMAL-[CSI]7m-INVERSE[CSI]1m-INV/BOLD-INV/BOLD[CSI]22;27m-NORMAL[CSI]m"


    ;; Basic tests for width:

    '("START |"
       ^{:ansi/width 10
         :ansi/pad :right} [nil "AAA"]
       "|"
       ^{:ansi/width 10} [nil "BBB"]
       "|")
    "START |AAA       |       BBB|"
    ;       0123456789 0123456789

    '("START |"
       ^{:ansi/width 10
         :ansi/pad :right} [:green "A" "A" "A"]
       "|"
       ^{:ansi/width 10} [:red "BBB"]
       "|")
    "START |[CSI]32mAAA       [CSI]39m|       [CSI]31mBBB[CSI]39m|[CSI]m"
    ;               0123456789         0123456789



    '("START |"
       ^{:ansi/width 10
         :ansi/pad :right} [:green "A" [nil "B"] [:blue "C"]]
       "|"
       ^{:ansi/width 10} [:red "XYZ"]
       "|")
    "START |[CSI]32mAB[CSI]34mC       [CSI]39m|       [CSI]31mXYZ[CSI]39m|[CSI]m"
    ;                       0123456789         0123456789

    ;; Only pads, never truncates

    '("START-" ^{:ansi/width 5} [nil "ABCDEFGH"] "-END")

    "START-ABCDEFGH-END"))

(deftest ignores-fonts-when-color-disabled
  (binding [ansi/*color-enabled* false]
    (is (= "Warning: Reactor Leak!"
           (compose [:red "Warning:"] " " [:bold "Reactor Leak!"])))))

(deftest unrecognized-font-modifier
  (when-let [e (is (thrown? Throwable (compose [:what.is.this? "Fail!"])))]
    (is (= "unexpected font term: :what" (ex-message e)))
    (is (= {:font-term :what
            :font-def :what.is.this?
            :available-terms [:black
                              :black-bg
                              :blue
                              :blue-bg
                              :bold
                              :bright-black
                              :bright-black-bg
                              :bright-blue
                              :bright-blue-bg
                              :bright-cyan
                              :bright-cyan-bg
                              :bright-green
                              :bright-green-bg
                              :bright-magenta
                              :bright-magenta-bg
                              :bright-red
                              :bright-red-bg
                              :bright-white
                              :bright-white-bg
                              :bright-yellow
                              :bright-yellow-bg
                              :cyan
                              :cyan-bg
                              :faint
                              :green
                              :green-bg
                              :inverse
                              :italic
                              :magenta
                              :magenta-bg
                              :normal
                              :not-underlined
                              :plain
                              :red
                              :red-bg
                              :roman
                              :underlined
                              :white
                              :white-bg
                              :yellow
                              :yellow-bg]}
          (ex-data e)))))


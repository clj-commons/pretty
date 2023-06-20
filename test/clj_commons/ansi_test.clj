(ns clj-commons.ansi-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is are]]
            [clj-commons.ansi :refer [csi compose]]))

(deftest compose-test
  (are [input expected]
    (= expected
      (-> (apply compose input)
        (str/replace csi "[CSI]")))

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

    ;; Demonstrate some optimizations (no change between first and second
    ;; INV/BOLD), and also specifying multiple font modifiers.

    ["NORMAL-"
     [:inverse "-INVERSE" [:bold "-INV/BOLD"]]
     [:inverse.bold "-INV/BOLD"]
     "-NORMAL"]
    "NORMAL-[CSI]7m-INVERSE[CSI]1m-INV/BOLD-INV/BOLD[CSI]22m[CSI]27m-NORMAL[CSI]m"))

(deftest unrecognized-font-modifier
  (when-let [e (is (thrown? Throwable (compose [:what.is.this? "Fail!"])))]
    (is (= "Unexpected font term: :what" (ex-message e)))
    (is (= {:font-term :what
            :font-def :what.is.this?
            :available-terms [:black
                              :black-bg
                              :blue
                              :blue-bg
                              :bold
                              :bright-black
                              :bright-black-gb
                              :bright-blue
                              :bright-blue-gb
                              :bright-cyan
                              :bright-cyan-gb
                              :bright-green
                              :bright-green-gb
                              :bright-magenta
                              :bright-magenta-gb
                              :bright-red
                              :bright-red-gb
                              :bright-white
                              :bright-white-gb
                              :bright-yellow
                              :bright-yellow-gb
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


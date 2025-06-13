(ns clj-commons.ansi-test
  (:require [clj-commons.ansi :as ansi]
            [clj-commons.test-common :as tc]
            [clojure.string :as str]
            [clojure.test :refer [deftest is are use-fixtures]]
            [clj-commons.ansi :refer [compose *color-enabled*]]
            [clj-commons.pretty-impl :refer [csi]]))

(use-fixtures :once tc/spec-fixture)

(deftest sanity-check
  (is (= true *color-enabled*)))

(defn- safe-compose
  [input]
  (-> (apply compose input)
      (str/replace csi "[CSI]")))

(deftest nested-widths
  (is (= "xyz <(      left)right             >"
         ;      ..........
         ;      ....!....! [10]
         ;     xxxxXxxxxXxxxxXxxxxXxxxxXxxxxX [30]
         (compose "xyz <"
                  [{:width 30
                    :align :left}
                   (list "("
                         [{:width 10}
                          "left"]
                         ")")
                   "right"]
                  ">"))))

(deftest invalid-font-decl
  (when-let [e (is (thrown? Exception
                            (compose "START"
                                     ["A" "B" "C"])))]
    (is (= "invalid span declaration"
           (ex-message e)))
    (is (= {:font-decl "A"}
           (ex-data e)))))

(deftest compose-test
  (are [input expected]
    (= expected (safe-compose input))

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
    ["Prefix--" [:bold nil ""] "--Suffix"]
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
    "Notice: the [CSI]0;33mshields[CSI]m are operating at [CSI]0;32m98.7%[CSI]m."

    ;; nil is allowed (this is used when formatting is optional, such as the fonts in exceptions).

    ["NORMAL" [nil "-STILL NORMAL"]]
    "NORMAL-STILL NORMAL"


    ;; Demonstrate some optimizations (no change between first and second
    ;; INV/BOLD), and also specifying multiple font modifiers.

    ["NORMAL"
     [:red "-RED"]
     [:bright-red "-BR/RED"]]
    "NORMAL[CSI]0;31m-RED[CSI]0;91m-BR/RED[CSI]m"

    ["NORMAL-"
     [:inverse "-INVERSE" [:bold "-INV/BOLD"]]
     [:inverse.bold "-INV/BOLD"]
     "-NORMAL"]
    "NORMAL-[CSI]0;7m-INVERSE[CSI]0;1;7m-INV/BOLD-INV/BOLD[CSI]m-NORMAL"


    ;; Basic tests for width:

    '("START |"
       [{:width 10
         :pad :right} "AAA"]
       "|"
       [{:width 10} "BBB"]
       "|")
    "START |AAA       |       BBB|"
    ;       0123456789 0123456789

    '("START |"
       [{:width 10
         :pad :right
         :font :green} "A" "A" "A"]
       "|"
       [{:width 10
         :font :red} "BBB"]
       "|")
    "START |[CSI]0;32mAAA       [CSI]m|[CSI]0;31m       BBB[CSI]m|"
    ;                 0123456789                 0123456789

    '("START |"
       [{:width 10
         :pad :right
         :font :green} "A" [nil "B"] [:blue "C"]]
       "|"
       [{:width 10
         :font :red} "XYZ"]
       "|")
    "START |[CSI]0;32mAB[CSI]0;34mC[CSI]0;32m       [CSI]m|[CSI]0;31m       XYZ[CSI]m|"
    ;                 01          2          3456789                 0123456789

    ;; Only pads, never truncates

    '("START-" [{:width 5} "ABCDEFGH"] "-END")

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
                              :crossed
                              :cyan
                              :cyan-bg
                              :double-underlined
                              :faint
                              :green
                              :green-bg
                              :inverse
                              :italic
                              :magenta
                              :magenta-bg
                              :normal
                              :not-crossed
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

(deftest unrecognized-vector-font-modifier
  (when-let [e (is (thrown? Throwable (compose [[:what :is :this?] "Fail!"])))]
    (is (= "unexpected font term: :what" (ex-message e)))
    (is (match? {:font-term :what
                 :font-def [:what :is :this?]}
                (ex-data e)))))

(deftest pad-both-even-padding
  (is (= "|    XX    |"
         ; ....  ....
         (compose "|" [{:width 10
                        :align :center}
                       "XX"] "|"))))

(deftest pad-both-odd-padding
  (is (= "|     X    |"
         ; .....  ....
         (compose "|" [{:width 10
                        :align :center}
                       "X"] "|"))))

(deftest use-of-vector-font-defs
  (doseq [[font-kw font-vector] [[:red.green-bg [:red :green-bg]]
                                 [:blue.italic [:italic :blue]]
                                 [:green.underlined [:green nil :underlined]]]]
    (is (= (compose [font-kw "some text"])
           (compose [font-vector "some text"])))


    (is (= (compose [{:font font-kw} "text"])
           (compose [{:font font-vector} "text"])))))

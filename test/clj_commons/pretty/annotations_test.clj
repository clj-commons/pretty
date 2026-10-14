(ns clj-commons.pretty.annotations-test
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.test :refer [deftest is use-fixtures]]
            [clj-commons.ansi :as ansi :refer [pout]]
            [clj-commons.test-common :as tc :refer [output-lines with-no-color]]
            [clj-commons.pretty.annotations :as a :refer [callouts default-style annotate-lines]]
            [matcher-combinators.test :refer [match?]]
            [matcher-combinators.matchers :as m]
            [clojure.spec.alpha :as s]
            [expound.alpha :as expound])
  (:import (clojure.lang ExceptionInfo)))

(defn expound-fixture
  [f]
  (binding [s/*explain-out* expound/printer]
    (f)))

(use-fixtures :once tc/spec-fixture expound-fixture)

(defn- compose-each
  [coll]
  (mapv ansi/compose coll))

(defn compose-all
  [& strings]
  (compose-each strings))

(deftest with-default-style
  ;; Ultimately, comparing the strings with ANSI characters (the result of compose).
  ;; This is a little bit unwieldy, as the implementation should have the option
  ;; for the font (the :yellow part) to just wrap around the markers,
  ;; bar, nib, and text, rather than around the whole line, including spaces.
  (is (match? (m/via compose-each
                     (compose-all
                       [:yellow "   ▲  ▲"]
                       [:yellow "   │  │"]
                       [:yellow "   │  └╴ Second"]
                       [:yellow "   └╴ First"]))
              (callouts [{:offset  3
                          :message "First"}
                         {:offset  6
                          :message "Second"}]))))

(deftest with-marker-function
  ;; Ultimately, comparing the strings with ANSI characters (the result of compose).
  (is (match? (m/via compose-each
                     (compose-all
                       [:yellow "   ▲  ┯━━━"]
                       [:yellow "   │  │"]
                       [:yellow "   │  └╴ Second"]
                       [:yellow "   └╴ First"]))
              (callouts [{:offset  3
                          :message "First"}
                         {:offset  6
                          :length  4
                          :marker  a/underline-marker
                          :message "Second"}]))))

(deftest sorts-by-offset
  (is (match? (m/via compose-each
                     (compose-all
                       [:yellow "   ▲  ▲"]
                       [:yellow "   │  │"]
                       [:yellow "   │  └╴ First"]
                       [:yellow "   └╴ Second"]))
              (callouts [{:offset  6
                          :message "First"}
                         {:offset  3
                          :message "Second"}]))))

(deftest with-annotation-length
  (is (match? (m/via compose-each
                     (compose-all
                       [:yellow "   ▲▲ ▲▲▲▲"]
                       [:yellow "   │  │"]
                       [:yellow "   │  └╴ First"]
                       [:yellow "   └╴ Second"]))
              (callouts [{:offset  6
                          :length  4
                          :message "First"}
                         {:offset  3
                          :length  2
                          :message "Second"}]))))

(deftest with-annotation-font
  (is (match? (m/via compose-each
                     (compose-all
                       [:yellow "   ▲▲ " [:red "▲▲▲▲"]]
                       [:yellow "   │  " [:red "│"]]
                       [:yellow "   │  " [:red "└╴ First"]]
                       [:yellow "   └╴ Second"]))
              (callouts [{:offset  6
                          :length  4
                          :font    :red
                          :message "First"}
                         {:offset  3
                          :length  2
                          :message "Second"}]))))

(deftest wrapping
  (is (match? (m/via compose-each
                     (compose-all
                       [:yellow "   ▲▲ " [:red "▲▲▲▲"]]
                       [:yellow "   │  " [:red "│"]]
                       [:yellow "   │  " [:red "└╴ This is a long message that will"]]
                       [:yellow "   │     " [:red "have to be word-wrapped."]]
                       [:yellow "   └╴ This " [:bold "second"] " message must also be"]
                       [:yellow "      split across lines."]))
              (callouts a/*default-style*
                        [{:offset  6
                          :length  4
                          :font    :red
                          :message "This is a long message that will have to be word-wrapped."}
                         {:offset  3
                          :length  2
                          :message (list "This " [:bold "second"] " message must also be split across lines.")}]
                        {:width 40}))))

(deftest spacing-minimal
  (is (match? (m/via compose-each
                     (compose-all
                       [:yellow "   ▲  ▲"]
                       [:yellow "   │  └╴ Second"]
                       [:yellow "   └╴ First"]))
              (callouts (assoc default-style :spacing :minimal)
                        [{:offset  3
                          :message "First"}
                         {:offset  6
                          :message "Second"}]))))

(deftest spacing-tall
  (is (match? (m/via compose-each
                     (compose-all
                       [:yellow "   ▲  ▲"]
                       [:yellow "   │  │"]
                       [:yellow "   │  └╴ Second"]
                       [:yellow "   │"]
                       [:yellow "   └╴ First"]))
              (callouts (assoc default-style :spacing :tall)
                        [{:offset  3
                          :message "First"}
                         {:offset  6
                          :message "Second"}]))))

(deftest custom-style
  (is (match? (m/via compose-each
                     (compose-all
                       [:blue "   ~  ~~"]
                       [:blue "   !  !"]
                       [:blue "   !  +> Second"]
                       [:blue "   +> First"]))
              (callouts {:font    :blue
                         :marker  "~"
                         :bar     "!"
                         :nib     "+> "
                         :bend    "+-+"
                         :spacing :compact}
                        [{:offset  3
                          :message "First"}
                         {:offset  6
                          :length 2
                          :message "Second"}]))))

(deftest annotate-lines-defaults-to-line-one
  (is (match? (m/via compose-each
                     (compose-all
                       "1: barney"
                       "2: fred"))
              (annotate-lines [{:line "barney"}
                               {:line "fred"}]))))

(deftest sets-line-number-column-width-from-max
  (is (match? (m/via compose-each
                     (compose-all
                       " 99: barney"
                       "100: fred"
                       "101: wilma"))
              (annotate-lines {:start-line 99}
                              [{:line "barney"}
                               {:line "fred"}
                               {:line "wilma"}]))))

(deftest can-omit-line-numbers
  (is (match? (m/via compose-each
                     (compose-all
                       "barney"
                       "fred"
                       "wilma"))
              (annotate-lines {:line-number-width 0}
                              [{:line "barney"}
                               {:line "fred"}
                               {:line "wilma"}]))))

(deftest intersperses-with-indented-annotation-lines
  (is (match? (m/via compose-each
                     (compose-all
                       [nil " 99: barney"]
                       [nil "     " [:yellow "  ▲"]]
                       [nil "     " [:yellow "  │"]]
                       [nil "     " [:yellow "  └╴ r not allowed"]]
                       "100: fred"
                       [nil "     " [:yellow "   ▲"]]
                       [nil "     " [:yellow "   │"]]
                       [nil "     " [:yellow "   └╴ d not allowed"]]
                       "101: wilma"))
              (annotate-lines {:start-line 99}
                              [{:line        "barney"
                                :annotations [{:offset 2 :message "r not allowed"}]}
                               {:line        "fred"
                                :annotations [{:offset 3 :message "d not allowed"}]}
                               {:line "wilma"}]))))

(deftest can-override-style
  (is (match? (m/via compose-each
                     (compose-all
                       [nil " 99: barney"]
                       [nil "     " [:blue "  ▲"]]
                       [nil "     " [:blue "  │"]]
                       [nil "     " [:blue "  └╴ r not allowed"]]
                       "100: fred"))
              (annotate-lines {:start-line 99
                               :style (assoc default-style :font :blue)}
                              [{:line        "barney"
                                :annotations [{:offset 2 :message "r not allowed"}]}
                               {:line        "fred"}]))))

(deftest can-override-line-number-width
  (is (match? (m/via compose-each
                     (compose-all
                       [nil "   99: barney"]
                       [nil "       " [:blue "  ▲"]]
                       [nil "       " [:blue "  │"]]
                       [nil "       " [:blue "  └╴ r not allowed"]]
                       "  100: fred"))
              (annotate-lines {:start-line 99
                               :line-number-width 5
                               :style (assoc default-style :font :blue)}
                              [{:line        "barney"
                                :annotations [{:offset 2 :message "r not allowed"}]}
                               {:line        "fred"}]))))

(def ^:private extend-marker #'a/extend-marker)

(deftest tri-character-marker
  (is (match? ["["
               "[]"
               "[-]"
               "[--]"]
              (for [i (range 1 5)]
                (extend-marker "[-]" i)))))

(deftest wrong-marker-length
  (is (thrown-with-msg? ExceptionInfo #"\QMarker string must be 1 or 3 characters\E"
                        (extend-marker "ab" 5))))

(deftest wrong-marker-type
  (is (thrown-with-msg? ExceptionInfo #"Marker should be a function or a string"
                        (extend-marker 0 5))))

(defn- render-set-no-color
  [annotation-set]
  (output-lines
    (with-no-color
      (run! pout (a/render-annotation-set annotation-set)))))

(def base-of-tower-lines
  ;; Make believe that we're annotating Dialog code.
  (-> "base-of-tower.dg"
      io/resource
      slurp
      string/split-lines))

(deftest annotation-set
  (let [annotation-set (-> base-of-tower-lines
                           (a/create-annotation-set)
                           ;; lines and offsets are 0 indexed, even though presented as 1 indexed
                           ;; Order of callouts never matters
                           (a/add-callout 64 {:offset 1} "is the color relevant?")
                           (a/add-callout 6 {:offset 5} "this is a then")
                           (a/add-callout 13 {:offset 8} "not a comment")
                           (a/add-callout 64 {:offset 13 :length 5} "not heavy"))]
    ;; This captures overlapping windows, the window cutting off at the last line (65),
    ;; multiple annotations on a single line.
    (is (match? [" 5:     Behind the concrete building a steel girder rises from the sand"
                 " 6:     (if) (lit by flashlight *)"
                 " 7:     (then)"
                 "         ▲"
                 "         │"
                 "         └╴ this is a then"
                 " 8:         ; you shine your flashlight up towards the tower looming above you"
                 " 9:     (elseif) (lit by headlights *)"
                 "12:         some gargantuan spider into blackness above you"
                 "13:     (else)"
                 "14:         ; far above you, a red warning light blinks on and off"
                 "            ▲"
                 "            │"
                 "            └╴ not a comment"
                 "15:     (endif)"
                 "16:     ."
                 "63:     Bits of dirty paper and cardboard, tiny scraps of wire, broken bits of plastic, and other indistinguishable and useless things."
                 "64: "
                 "65: #red-warning-light"
                 "     ▲           ▲▲▲▲▲"
                 "     │           │"
                 "     │           └╴ not heavy"
                 "     └╴ is the color relevant?"
                 "66: (name *) red warning light"]
                (render-set-no-color annotation-set)))))

(deftest annotation-set-override-defaults
  (let [annotation-set (-> base-of-tower-lines
                           (a/create-annotation-set {:line-offset 101
                                                     :window      5
                                                     :style       (assoc a/default-style
                                                                    :spacing :minimal
                                                                    :marker a/underline-marker)})
                           (a/add-callout 2 {:offset 9
                                             :length 4} "All your base are belong to us")
                           (a/add-callout 2 {:offset 1} "Mark your time"))]
    (is (match? ["101: #base-of-tower"
                 "102: (around the tower *)"
                 "103: (name *) Base of the Tower"
                 "      ┯       ┯━━━"
                 "      │       └╴ All your base are belong to us"
                 "      └╴ Mark your time"
                 "104: (look *)"
                 "105:     Behind the concrete building a steel girder rises from the sand"
                 "106:     (if) (lit by flashlight *)"
                 "107:     (then)"
                 "108:         ; you shine your flashlight up towards the tower looming above you"]
                (render-set-no-color annotation-set)))))

(deftest annotation-set-with-width
  (let [annotation-set (-> base-of-tower-lines
                           (a/create-annotation-set {:width 40
                                                     :reroute-width 1})
                           ;; lines and offsets are 0 indexed, even though presented as 1 indexed
                           ;; Order of callouts never matters
                           (a/add-callout 64 {:offset 1} "is the color relevant?")
                           (a/add-callout 6 {:offset 5} "it's important to understand that this is not a predicate, it's more like a special form.")
                           (a/add-callout 13 {:offset 8} "not a comment, this is a semicolon in the literate sense")
                           (a/add-callout 64 {:offset 13 :length 5} "not heavy"))]
    ;; At this time, the fact that the lines themselves are NOT wrapped is intentional.
    (is (match? [" 5:     Behind the concrete building a steel girder rises from the sand"
                 " 6:     (if) (lit by flashlight *)"
                 " 7:     (then)"
                 "         ▲"
                 "         │"
                 ;          1         2         3         4
                 ;01234567890123456789012345678901234567890 - 39 is last offset
                 "         └╴ it's important to understand"
                 "            that this is not a predicate,"
                 "            it's more like a special"
                 "            form."
                 " 8:         ; you shine your flashlight up towards the tower looming above you"
                 " 9:     (elseif) (lit by headlights *)"
                 "12:         some gargantuan spider into blackness above you"
                 "13:     (else)"
                 "14:         ; far above you, a red warning light blinks on and off"
                 "            ▲"
                 "            │"
                 ;          1         2         3         4
                 ;01234567890123456789012345678901234567890
                 "            └╴ not a comment, this is a"
                 "               semicolon in the literate"
                 "               sense"
                 "15:     (endif)"
                 "16:     ."
                 "63:     Bits of dirty paper and cardboard, tiny scraps of wire, broken bits of plastic, and other indistinguishable and useless things."
                 "64: "
                 "65: #red-warning-light"
                 "     ▲           ▲▲▲▲▲"
                 "     │           │"
                 "     │           └╴ not heavy"
                 "     └╴ is the color relevant?"
                 "66: (name *) red warning light"]
                (render-set-no-color annotation-set)))))

(deftest annotation-set-with-reroute
  (let [annotation-set (-> base-of-tower-lines
                           (a/create-annotation-set {:width 40 :window 1})
                           (a/add-callout 5 {:offset 9} "check to see if the player can see in the current environment")
                           (a/add-callout 5 {:offset 28} "* is always a reference to the most recently declared topic"))]
    (is (match? ["5:     Behind the concrete building a steel girder rises from the sand"
                 "6:     (if) (lit by flashlight *)"
                 ;          1         2         3         4
                 ;01234567890123456789012345678901234567890 - offset by 3
                 "            ▲                  ▲"
                 "            │                  │"
                 "   ┌────────┘                  │"
                 "   │ ┌─────────────────────────┘"
                 "   │ └╴ * is always a reference to the"
                 "   │    most recently declared topic"
                 "   └╴ check to see if the player can see"
                 "      in the current environment"
                 "7:     (then)"]
                (render-set-no-color annotation-set)))))

(deftest annotation-set-with-partial-reroute
  (let [annotation-set (-> base-of-tower-lines
                           (a/create-annotation-set {:width 40 :window 1})
                           (a/add-callout 5 {:offset 0} "pointing at whitespace (offset 0)")
                           (a/add-callout 5 {:offset 28} "* is always a reference to the most recently declared topic"))]
    (is (match?  ["5:     Behind the concrete building a steel girder rises from the sand"
                  "6:     (if) (lit by flashlight *)"
                  "   ▲                           ▲"
                  "   │                           │"
                  "   │                           │"
                  "   │ ┌─────────────────────────┘"
                  "   │ └╴ * is always a reference to the"
                  "   │    most recently declared topic"
                  "   └╴ pointing at whitespace (offset 0)"
                  "7:     (then)"]
                (render-set-no-color annotation-set)))))

(comment

  (let [annotation-set (-> base-of-tower-lines
                           (a/create-annotation-set {:line-offset 101
                                                     :window      5
                                                     :width       30
                                                     :style       (assoc a/default-style
                                                                    :spacing :minimal
                                                                    :marker a/underline-marker)})
                           (a/add-callout 2 {:offset 9
                                             :length 4
                                             :font   :bold.green} "All your base are belong to us")
                           (a/add-callout 2 {:offset 1} "Mark your time"))]
    (run! pout (a/render-annotation-set annotation-set)))
  ;
  )
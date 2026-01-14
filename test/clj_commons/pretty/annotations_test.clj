(ns clj-commons.pretty.annotations-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clj-commons.ansi :as ansi]
            [clj-commons.test-common :as tc]
            [clj-commons.pretty.annotations :as a :refer [callouts default-style annotate-lines]]
            [matcher-combinators.test :refer [match?]]
            [matcher-combinators.matchers :as m])
  (:import (clojure.lang ExceptionInfo)))

(use-fixtures :once tc/spec-fixture)

(defn- compose-each
  [coll]
  (mapv ansi/compose coll))

(defn compose-all
  [& strings]
  (compose-each strings))

(deftest with-default-style
  ;; Ultimately, comparing the strings with ANSI characters (the result of compose).
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

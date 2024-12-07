(ns clj-commons.pretty.annotations-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clj-commons.ansi :as ansi]
            [clj-commons.test-common :as tc]
            [clj-commons.pretty.annotations :refer [callouts default-style]]
            [matcher-combinators.matchers :as m]))

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
                       [:yellow "   │"]
                       [:yellow "   └╴ First"]))
              (callouts [{:offset  3
                          :message "First"}
                         {:offset  6
                          :message "Second"}]))))

(deftest sorts-by-offset
  (is (match? (m/via compose-each
                     (compose-all
                       [:yellow "   ▲  ▲"]
                       [:yellow "   │  │"]
                       [:yellow "   │  └╴ First"]
                       [:yellow "   │"]
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
                       [:yellow "   │"]
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
                       [:yellow "   │"]
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

(deftest spacing-compact
  (is (match? (m/via compose-each
                     (compose-all
                       [:yellow "   ▲  ▲"]
                       [:yellow "   │  │"]
                       [:yellow "   │  └╴ Second"]
                       [:yellow "   └╴ First"]))
              (callouts (assoc default-style :spacing :compact)
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
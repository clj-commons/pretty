(ns clj-commons.table-test
  (:require [clj-commons.test-common :as tc]
            [clojure.string :as string]
            [clojure.test :refer [deftest is use-fixtures]]
            [clj-commons.ansi :as ansi]
            [clj-commons.format.table :as table]))

(use-fixtures :once tc/spec-fixture)

(def sample-rows [{:first "Arthur" :middle "C" :last "Clark"}
                  {:first "Alan" :last "Turing"}
                  {:first "Larry" :last "Niven"}
                  {:first "Fred" :last "Flintstone"}])

(defn- capture-table
  [opts rows]
  (-> (binding [ansi/*color-enabled* false]
        (with-out-str
          (table/print-table opts rows)))
      string/split-lines))

(deftest default-style
  (is (= ["┌──────┬──────┬──────────┐"
          "│ First│Middle│   Last   │"
          "├──────┼──────┼──────────┤"
          "│Arthur│     C│Clark     │"
          "│  Alan│      │Turing    │"
          "│ Larry│      │Niven     │"
          "│  Fred│      │Flintstone│"
          "└──────┴──────┴──────────┘"]
         (capture-table [:first :middle :last] sample-rows))))

(deftest skinny-style
  (is (= [" First | Middle |    Last   "
          "-------+--------+-----------"
          "Arthur |      C | Clark     "
          "  Alan |        | Turing    "
          " Larry |        | Niven     "
          "  Fred |        | Flintstone"]
         (capture-table {:columns [:first :middle :last]
                         :style   table/skinny-style}
                        sample-rows))))

(deftest minimal-style
  (is (= [" First Middle    Last   "
          "Arthur      C Clark     "
          "  Alan        Turing    "
          " Larry        Niven     "
          "  Fred        Flintstone"]
         (capture-table {:columns [:first :middle :last]
                         :style   table/minimal-style}
                        sample-rows))))

(deftest markdown-style
  (is (= ["|  First | Middle |    Last    |"
          "|--------|--------|------------|"
          "| Arthur |      C | Clark      |"
          "|   Alan |        | Turing     |"
          "|  Larry |        | Niven      |"
          "|   Fred |        | Flintstone |"]
         (capture-table {:columns [:first :middle :last]
                         :style   table/markdown-style}
                        sample-rows))))

(deftest markdown-style-single-column
  (is (= ["|  First |"
          "|--------|"
          "| Arthur |"
          "| Alan   |"
          "| Larry  |"
          "| Fred   |"]
         (capture-table {:columns [:first]
                         :style   table/markdown-style}
                        sample-rows))))

(deftest markdown-style-with-explicit-titles
  (is (= ["| Given name | Family name |"
          "|------------|-------------|"
          "| Arthur     | Clark       |"
          "| Alan       | Turing      |"
          "| Larry      | Niven       |"
          "| Fred       | Flintstone  |"]
         (capture-table {:columns [{:key :first :title "Given name" :align :left}
                                   {:key :last :title "Family name"}]
                         :style   table/markdown-style}
                        sample-rows))))

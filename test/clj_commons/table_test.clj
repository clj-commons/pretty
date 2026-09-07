(ns clj-commons.table-test
  (:require [clj-commons.test-common :as tc]
            clj-commons.format.table.specs
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

(deftest columns-with-wrap
  (is (= ["        Title        |  Genre |             Summary           "
          "---------------------+--------+-------------------------------"
          " I Have No Mouth And | horror | A maleovolent AI physically an *"
          "       I Must Scream |        | d psychologically tortures the"
          "                     |        | last remaining humans.        "
          " The Lathe Of Heaven |     sf | A man whose convinced that his *"
          "                     |        | dreams come true seeks the car"
          "                     |        | e of a doctor                 "
          ;; The space follows the newline and is kept:
          "                     |        |  who wishes to take that power"
          "                     |        | for himself.                  "]
         (capture-table {:columns       [{:key   :title
                                          :width 20
                                          :wrap  :soft}
                                         :genre
                                         {:key   :summary
                                          :width 30
                                          :wrap  :hard}]
                         :row-annotator (constantly " *")
                         :style         table/skinny-style}
                        [{:title   "I Have No Mouth And I Must Scream"
                          :genre   "horror"
                          :summary "A maleovolent AI physically and psychologically tortures the last remaining humans."}
                         {:title   "The Lathe Of Heaven"
                          :genre   "sf"
                          :summary "A man whose convinced that his dreams come true seeks the care of a doctor\n who wishes to take that power for himself."}]))))

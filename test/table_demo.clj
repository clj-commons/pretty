(ns table-demo
  (:require [clj-commons.format.table :refer [print-table] :as t]))

(def row [{:first "Arthur" :middle "C" :last "Clark"}
          {:first "Alan" :last "Turing"}
          {:first "Larry" :last "Niven"}
          {:first "Fred" :last "Flintstone"}])
(def columns [:first
              {:key   :middle
               :width 15
               :align :right}
              {:key       :last
               :title     "Family Name"
               :decorator (fn [i v]
                            (when (odd? i)
                              :bold))}])
(comment
  (print-table columns row)
  (print-table {:style   t/skinny-style
                :default-decorator
                (fn [i _]
                  (when (odd? i)
                    :blue))
                :columns columns} row)

  )
(ns table-demo
  (:require [clj-commons.format.table :refer [print-table] :as t]))

(def row [{:first "Arthur" :middle "C" :last "Clark"}
          {:first "Alan" :last "Turing"}
          {:first "Larry" :last "Niven"}
          {:first "Fred" :last "Flintstone"}])
(def columns [:first
              {:key   :middle
               :width 15                                    ; much too wide, for testing purposes
               :align :right}
              {:key       :last
               :title     "Family Name"
               :decorator (fn [i v]
                            (when (odd? i)
                              :bold))}])
(comment
  (print-table columns row)
  (print-table {:style t/minimal-style :columns columns} row)
  (print-table {:style         t/skinny-style
                :row-annotator (fn [i row]
                                 (when (= i 2)
                                   [:italic " (prescient)"]))
                :default-decorator
                (fn [i _]
                  (when (odd? i)
                    :blue))
                :columns       columns} row)

  (print-table
    [:method
     :path
     {:key :route-name :title "Name" :title-align :left}]
    [{:method     :get
      :path       "/"
      :route-name :root-page}
     {:method     :post
      :path       "/reset"
      :route-name :reset}
     {:method     :get
      :path       "/status"
      :route-name :status}])

   )

(ns playground
  (:require [clj-commons.ansi :as ansi]
            [clj-commons.format.exceptions :as e]))

(defn deprecation-warning
  [id]
  (let [names (->> (Thread/currentThread)
                   .getStackTrace
                   (drop 1)                                 ; call to .getStackTrace()
                   (e/transform-stack-trace)
                   (e/filter-stack-trace-maps)
                   (drop-while #(= "playground/deprecation-warning" (:name %)))
                   (remove :omitted)
                   (map e/format-stack-frame)
                   (map :name)
                   reverse
                   (interpose " -> "))]
    (ansi/perr [:yellow
                [:bold "WARNING:"]
                " " id " is deprecated ...\n"]
               "Call trace: "
               names)))

(defmacro deprecated
  [id & body]
  `(do
     (deprecation-warning ~id)
     ~@body))

(defn my-deprecated-fn
  []
  (deprecated `my-deprecated-fn))

(defn caller
  []
  (my-deprecated-fn))

(defn deep
  [x]
  ((fn [x1]
     ((fn [x2]
        ((fn [x3]
           ((fn inner [x4]
              (/ x4 0)) x3))
         x2))
      x1))
   x))

(comment
  (caller)

  (deep 10)

  (clojure.repl/pst)

  )

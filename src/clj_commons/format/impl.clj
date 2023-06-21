(ns ^:no-doc clj-commons.format.impl
  "Private/internal - subject to change without notice.")


(defn padding [x]
  (if (zero? x)
    nil
    (let [sb (StringBuilder. (int x))]
      (dotimes [_ x]
        (.append sb " "))
      (.toString sb))))

(ns ^:no-doc clj-commons.pretty-impl
  "Private/internal - subject to change without notice.")

(defn padding [x]
  (if (zero? x)
    nil
    (let [sb (StringBuilder. (int x))]
      (dotimes [_ x]
        (.append sb " "))
      (.toString sb))))

(def ^:const csi
  "The control sequence initiator: `ESC [`"
  "\u001b[")

(ns io.aviso.exception
  "Code to assist with presenting exceptions in pretty way."
  (import [clojure.lang Compiler])
  (require [clojure.set :as set]))

;;; Obviously, this is making use of some internals of Clojure that
;;; could change at any time.

(def ^:private DEMUNGE
  (->> (Compiler/CHAR_MAP)
       set/map-invert
       (sort-by #(-> % first .length))
       reverse))

(defn- match-munged
  [^String s i]
  (->>
    DEMUNGE
    (filter (fn [[k _]] (.regionMatches s i k 0 (.length k))))
    first))

(defn demunge
  "De-munges a Java name back to a Clojure name by converting munged sequences, such as \"_QMARK_\"
  back into simple characters."
  [s]
  (let [in-length (.length s)
        result (StringBuilder. in-length)]
    (loop [i 0]
      (cond
        (>= i in-length) (.toString result)
        (= \_ (.charAt s i)) (let [[match replacement] (match-munged s i)]
                               (.append result replacement)
                               (recur (+ i (.length match))))
        :else (do
                (.append result (.charAt s i))
                (recur (inc i)))))))
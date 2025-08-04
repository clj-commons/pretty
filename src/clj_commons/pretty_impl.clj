(ns ^:no-doc clj-commons.pretty-impl
  "Private/internal - subject to change without notice.")

(defn padding
  ^String [x]
  (when (pos? x)
    (let [sb (StringBuilder. (int x))]
      (dotimes [_ x]
        (.append sb " "))
      (.toString sb))))

(def ^:const csi
  "The control sequence initiator: `ESC [`"
  "\u001b[")

(defn- matches-count
  [i window-width v]
  (let [[subs & more-subs] (partition window-width (subvec v i))]
    #_(prn :i i :w window-width :subs subs)
    (reduce (fn [c next-subs]
              #_(prn :c c :next next-subs)
              (if (= subs next-subs)
                (inc c)
                (reduced c)))
            1
            more-subs)))

(defn- find-subs
  [i n v]
  (let [remaining (- n i)
        max-width (Math/floorDiv ^long remaining 2)]
    ;; The maximium length of a subsequence is half of the remaining values
    (loop [window-width 1]
      (if (> window-width max-width)
        [1 (subvec v i (inc i))]
        (let [c (matches-count i window-width v)]
          (if (> c 1)
            [c (subvec v i (+ i window-width))]
            (recur (inc window-width))))))))

(defn repetitions
  "Identifies repetitions in a finite collection.  Returns a series of tuples of [count sub-seq].
  Values from the coll are uniquely identified by k."
  [k coll]
  (let [id->val (reduce (fn [m v]
                          (assoc m (k v) v))
                        {}
                        coll)
        v (mapv k coll)
        n (count v)]
    (loop [i 0
           result (transient [])]
      (if (>= i n)
        (persistent! result)
        (let [subs (find-subs i n v)
              [^long sub-count sub-ids] subs
              subs' [sub-count (mapv id->val sub-ids)]
              total-matches (* sub-count (count sub-ids))]
          (recur (+ i total-matches)
                 (conj! result subs')))))))

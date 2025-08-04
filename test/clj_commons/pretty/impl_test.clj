(ns clj-commons.pretty.impl-test
  (:require [clojure.test :refer [deftest is]]
            [clj-commons.pretty-impl :refer [repetitions]]))

(deftest repetitions-basic
  (is (= [[1 [:d]]
          [3 [:a :b :c]]
          [2 [:f :g]]
          [1 [:z]]]
         (repetitions identity [:d :a :b :c :a :b :c :a :b :c :f :g :f :g :z]))))

(deftest repetitions-via-key-id
  (let [a {:id 'a}
        b {:id 'b}
        c {:id 'c}]
    (is (= [[2 [a b]]
            [2 [c a b]]
            [2 [c]]
            [1 [b]]
            [1 [a]]]
           (repetitions :id [a b a b c a b c a b c c b a])))))

(deftest repetitions-when-empty
  (is (= []
         (repetitions :id []))))

(deftest repetitions-short-sequence
  (is (= [[1 [:a]]
          [1 [:b]]
          [15 [:c]]
          [1 [:d]]]
         (repetitions identity
                      [:a :b :c :c :c :c :c :c :c :c :c :c :c :c :c :c :c :d]))))

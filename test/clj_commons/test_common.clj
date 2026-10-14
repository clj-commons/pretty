(ns clj-commons.test-common
  (:require clj-commons.pretty.spec
            [clojure.spec.test.alpha :as stest]
            [clj-commons.ansi :as ansi]
            [clojure.string :as string]))

(defn spec-fixture
  [f]
  (try
    (stest/instrument)
    (f)
    (finally
      (stest/unstrument))))

(defn force-ansi-fixture
  [f]
  (binding [ansi/*color-enabled* true]
    (f)))

(defmacro output-lines
  [& body]
  `(let [out# (with-out-str ~@body)]
     (string/split-lines out#)))

(defmacro with-no-color
  [& body]
  `(binding [ansi/*color-enabled* false]
     ~@body))
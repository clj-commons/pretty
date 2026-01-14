(ns clj-commons.test-common
  (:require clj-commons.pretty.spec
            [clojure.spec.test.alpha :as stest]
            [clj-commons.ansi :as ansi]))

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

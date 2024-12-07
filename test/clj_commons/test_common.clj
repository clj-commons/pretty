(ns clj-commons.test-common
  (:require clj-commons.pretty.spec
            [clojure.spec.test.alpha :as stest]))

(defn spec-fixture
  [f]
  (try
    (stest/instrument)
    (f)
    (finally
      (stest/unstrument))))

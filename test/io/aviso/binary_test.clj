(ns io.aviso.binary-test
  "Tests for the io.aviso.binary namespace."
  (:use io.aviso.binary
        clojure.test))

(defn- format-string-as-byte-array [str]
  (format-byte-array (.getBytes str)))

(deftest format-byte-array-test

  (are [input output] (= (format-string-as-byte-array input) output)
                      "Hello" "0000: 48 65 6C 6C 6F\n"
                      "This is a longer text that spans to a second line."
                      "0000: 54 68 69 73 20 69 73 20 61 20 6C 6F 6E 67 65 72 20 74 65 78 74 20 74 68 61 74 20 73 70 61 6E 73\n0020: 20 74 6F 20 61 20 73 65 63 6F 6E 64 20 6C 69 6E 65 2E\n"))

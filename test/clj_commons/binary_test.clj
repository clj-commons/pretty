(ns clj-commons.binary-test
  "Tests for the clj-commons.format.binary namespace."
  (:require [clj-commons.ansi :as ansi]
            [clj-commons.format.binary :as b]
            [clojure.string :as string]
            [clojure.test :refer [deftest is are]])
  (:import (java.nio ByteBuffer)))

(defn- format-binary-plain
  [input]
  (binding [ansi/*color-enabled* false]
    (b/format-binary input)))

(defn- format-binary-delta-plain
  [expected actual]
  (binding [ansi/*color-enabled* false]
    (string/split-lines (b/format-binary-delta expected actual))))

(defn- fixup-sgr
  [s]
  (string/replace s #"\u001b\[(.*?)m" "{$1}"))

(defn- format-binary-delta
  [expected actual]
  (-> (b/format-binary-delta expected actual)
      fixup-sgr
      string/split-lines))

(defn- format-binary-string-plain
  [^String str]
  (format-binary-plain (.getBytes str)))

(deftest format-byte-array-test

  (are [input expected]
    (= expected (format-binary-string-plain input))

    "Hello" "0000: 48 65 6C 6C 6F\n"

    "This is a longer text that spans to a second line."
    "0000: 54 68 69 73 20 69 73 20 61 20 6C 6F 6E 67 65 72 20 74 65 78 74 20 74 68 61 74 20 73 70 61 6E 73\n0020: 20 74 6F 20 61 20 73 65 63 6F 6E 64 20 6C 69 6E 65 2E\n"))

(deftest binary-fonts
  (let [byte-data (byte-array [0x59 0x65 073 0x20 0x4e 0x00 0x00 0x09 0x80 0xff])]
    (is (= ["{90}0000:{39} {36}59{39} {36}65{39} {36}3B{39} {32}20{39} {36}4E{39} {90}00{39} {90}00{39} {32}09{39} {33}80{39} {33}FF{39}                   |{36}Ye;{32} {36}N{90}••{32}_{33}××{39}      |{}"]
           (-> (b/format-binary byte-data {:ascii true})
               fixup-sgr
               string/split-lines)))))

(deftest format-string-as-byte-data
  (are [input expected]
    (= expected (format-binary-plain input))
    "" ""

    "Hello" "0000: 48 65 6C 6C 6F\n"

    "This is a longer text that spans to a second line."
    "0000: 54 68 69 73 20 69 73 20 61 20 6C 6F 6E 67 65 72 20 74 65 78 74 20 74 68 61 74 20 73 70 61 6E 73\n0020: 20 74 6F 20 61 20 73 65 63 6F 6E 64 20 6C 69 6E 65 2E\n"))

(deftest nil-is-an-empty-data
  (is (= (format-binary-plain nil) "")))

(deftest byte-buffer
  (let [bb (ByteBuffer/wrap (.getBytes "Duty Now For The Future" "UTF-8"))]
    (is (= "0000: 44 75 74 79 20 4E 6F 77 20 46 6F 72 20 54 68 65 20 46 75 74 75 72 65\n"
           (format-binary-plain bb)))

    (is (= "0000: 44 75 74 79\n"
           (-> bb
               (.position 5)
               (.limit 9)
               format-binary-plain)))

    (is (= "0000: 46 6F 72\n"
           (-> bb
               (.position 9)
               (.limit 12)
               .slice
               format-binary-plain)))))

(deftest deltas
  (are [expected actual expected-output]
    (= expected-output
       (format-binary-delta-plain expected actual))

    "123" "123"
    ["0000: 31 32 33                                        | 31 32 33"]

    "abcdefghijklmnopqrstuvwyz" "abCdefghijklmnopqrs"
    ["0000: 61 62 63 64 65 66 67 68 69 6A 6B 6C 6D 6E 6F 70 | 61 62 43 64 65 66 67 68 69 6A 6B 6C 6D 6E 6F 70"
     "0010: 71 72 73 74 75 76 77 79 7A                      | 71 72 73 -- -- -- -- -- --"]

    "abc" "abcdef"
    ["0000: 61 62 63 -- -- --                               | 61 62 63 64 65 66"]))


(deftest deltas-with-fonts
  (are [expected actual expected-output]
    (= expected-output
       (format-binary-delta expected actual))

    "123\t" "123\n"
    ;; 90 is bright black for offset
    ;; 36 is cyan for printable ASCII
    ;; 32 is green for whitespace
    ;; 102 is bright green backround,
    ;; 101 is bright red background
    ["{90}0000:{39} {36}31{39} {36}32{39} {36}33{39} {32;102}09{39;49}                                     | {36}31{39} {36}32{39} {36}33{39} {32;101}0A{}"]

    "1234" "12"
    ["{90}0000:{39} {36}31{39} {36}32{39} {36;102}33{39;49} {36;102}34{39;49}                                     | {36}31{39} {36}32{39} {101}--{49} {101}--{}"]

    ;; 2 is faint for non-printable
    "\u001B" "\u001Cxyz"
    ["{90}0000:{39} {32;102;2}1B{39;49;22} {102}--{49} {102}--{49} {102}--{49}                                     | {32;101;2}1C{39;49;22} {36;101}78{39;49} {36;101}79{39;49} {36;101}7A{}"]))




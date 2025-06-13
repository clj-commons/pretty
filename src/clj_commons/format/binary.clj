(ns clj-commons.format.binary
  "Utilities for formatting binary data (byte arrays) or binary deltas."
  (:require [clj-commons.ansi :refer [pout]]
            [clj-commons.pretty-impl :refer [padding]])
  (:import (java.nio ByteBuffer)))

(def ^:dynamic *fonts*
  "Mapping from byte category to a font (color)."
  {:offset     :bright-black
   :null       :bright-black
   :printable  :cyan
   :whitespace :green
   :other      :faint.green
   :non-ascii  :yellow})

(def ^:private placeholders
  {:null       "•"
   :whitespace "_"
   :other      "•"
   :non-ascii  "×"})

(defprotocol BinaryData
  "Allows various data sources to be treated as a byte-array data type that
  supports a length and random access to individual bytes.

  BinaryData is extended onto byte arrays, java.nio.ByteBuffer, java.lang.String, java.lang.StringBuilder, and onto nil."

  (data-length [this] "The total number of bytes available.")
  ^byte (byte-at [this index] "The byte value at a specific offset."))

;; This is problematic for clj-kondo, but valid.
#_:clj-kondo/ignore
(extend-type (Class/forName "[B")
  BinaryData
  (data-length [ary] (alength (bytes ary)))
  (byte-at [ary index] (aget (bytes ary) index)))

(extend-type ByteBuffer
  BinaryData
  (data-length [b] (.remaining b))
  (byte-at [b index] (.get ^ByteBuffer b (int index))))

;;; Extends String as a convenience; assumes that the
;;; String is in utf-8.

(extend-type String
  BinaryData
  (data-length [s] (.length s))
  (byte-at [s index] (-> s (.charAt index) int byte)))

(extend-type StringBuilder
  BinaryData
  (data-length [sb] (.length sb))
  (byte-at [sb index]
    (-> sb (.charAt index) int byte)))

(extend-type nil
  BinaryData
  (data-length [_] 0)
  (byte-at [_ _index] (throw (IndexOutOfBoundsException. "can not use byte-at with nil"))))

(def ^:private ^:const bytes-per-diff-line 16)
(def ^:private ^:const bytes-per-ascii-line 16)
(def ^:private ^:const bytes-per-line (* 2 bytes-per-diff-line))

(def ^:private whitespace
  #{0x09 0x0a 0x0b 0x0c 0x0d 0x20})

(defn- category-for-byte
  [^long value]
  (cond
    (zero? value)
    :null

    (< 0x7f value)
    :non-ascii

    (contains? whitespace value)
    :whitespace

    (<= 0x21 value 0x7e)
    :printable

    :else
    :other))

(defn- font-for-byte
  [^long value]
  (get *fonts* (category-for-byte value)))

(defn- to-ascii
  [^long b]
  (let [category (category-for-byte b)]
    [(get *fonts* category)
     (if (or (= :printable category)
             (= 0x20 b))
       (char b)
       (get placeholders category))]))

(defn- hex-digit-count
  [max-length]
  (loop [digits 4
         cutoff 0xffff]
    (if (<= max-length cutoff)
      digits
      (recur (+ 2 digits)
             (* cutoff 0xff)))))

(defn- make-offset-format
  [max-length]
  (str "%0" (hex-digit-count max-length) "X:"))

(defn- write-line
  [write-ascii? offset-format offset data line-count per-line]
  (let [line-bytes (for [i (range line-count)]
                     (Byte/toUnsignedLong (byte-at data (+ offset i))))]
    (pout
      [(:offset *fonts*)
       (format offset-format offset)]
      (for [b line-bytes]
        (list " "
              [(font-for-byte b)
               (format "%02X" b)]))
      (when write-ascii?
        (list
          (padding (* 3 (- per-line line-count)))
          " |"
          (map to-ascii line-bytes)
          (padding (- per-line line-count))
          "|")))))

(defn print-binary
  "Formats a BinaryData into a hex-dump string, consisting of multiple lines; each line formatted as:

      0000: 43 68 6F 6F 73 65 20 69 6D 6D 75 74 61 62 69 6C 69 74 79 2C 20 61 6E 64 20 73 65 65 20 77 68 65
      0020: 72 65 20 74 68 61 74 20 74 61 6B 65 73 20 79 6F 75 2E

  The full version specifies the [[BinaryData]] to write, and options:


  Key         | Type    | Description
  ---         |---      |---
  :ascii      | boolean | If true, enable ASCII mode
  :line-bytes | number  | Bytes printed per line

  :line-bytes defaults to 16 for ASCII, and 32 otherwise.

  In ASCII mode, the output is 16 bytes per line, but each line includes the ASCII printable characters:

      0000: 43 68 6F 6F 73 65 20 69 6D 6D 75 74 61 62 69 6C |Choose immutabil|
      0010: 69 74 79 2C 20 61 6E 64 20 73 65 65 20 77 68 65 |ity, and see whe|
      0020: 72 65 20 74 68 61 74 20 74 61 6B 65 73 20 79 6F |re that takes yo|
      0030: 75 2E                                           |u.              |

  When ANSI is enabled, the individual bytes and characters are color-coded as per the [[*fonts*]]."
  ([data]
   (print-binary data nil))
  ([data options]
   (let [{show-ascii?     :ascii
          per-line-option :line-bytes} options
         per-line (or per-line-option
                      (if show-ascii? bytes-per-ascii-line bytes-per-line))
         input-length (data-length data)
         offset-format (make-offset-format input-length)]
     (assert (pos? per-line) "must be at least one byte per line")
     (loop [offset 0]
       (let [remaining (- input-length offset)]
         (when (pos? remaining)
           (write-line show-ascii? offset-format offset data (min per-line remaining) per-line)
           (recur (long (+ per-line offset)))))))))

(defn format-binary
  "Formats the data using [[write-binary]] and returns the result as a string."
  ([data]
   (format-binary data nil))
  ([data options]
   (with-out-str
     (print-binary data options))))

(defn- match?
  [byte-offset data-length data alternate-length alternate]
  (and
    (< byte-offset data-length)
    (< byte-offset alternate-length)
    (== (byte-at data byte-offset) (byte-at alternate byte-offset))))

(defn- compose-deltas
  "Returns a composed value of one line (16 bytes) of data."
  [mismatch-font offset data-length data alternate-length alternate]
  (for [i (range bytes-per-diff-line)]
    (let [byte-offset (+ offset i)
          *value (delay
                   (let [value (long (byte-at data byte-offset))
                         byte-font (font-for-byte value)]
                     [byte-font (format "%02X" value)]))]
      (cond
        (match? byte-offset data-length data alternate-length alternate)
        (list " " @*value)

        ;; Some kind of mismatch, so decorate with this side's color
        (< byte-offset data-length) (list " " [mismatch-font @*value])
        ;; Are we out of data on this side?  Print a "--" decorated with the color.
        (< byte-offset alternate-length) (list " " [mismatch-font "--"])))))

(defn- print-delta-line
  [offset-format offset expected-length expected actual-length actual]
  (pout
    [(:offset *fonts*)
     (format offset-format offset)]
    [{:align :left
      :width (* 3 bytes-per-diff-line)}
     (compose-deltas :bright-green-bg offset expected-length expected actual-length actual)]
    " |"
    (compose-deltas :bright-red-bg offset actual-length actual expected-length expected)))

(defn print-binary-delta
  "Formats a hex dump of the expected data (on the left) and actual data (on the right). Bytes
  that do not match are highlighted in green on the expected side, and red on the actual side.
  When one side is shorter than the other, it is padded with `--` placeholders to make this
  more clearly visible.

  expected and actual are [[BinaryData]].

  Display 16 bytes (from each data set) per line."
  [expected actual]
  (let [expected-length (data-length expected)
        actual-length (data-length actual)
        target-length (max actual-length expected-length)
        offset-format (make-offset-format (max actual-length target-length))]
    (loop [offset 0]
      (when (pos? (- target-length offset))
        (print-delta-line offset-format offset expected-length expected actual-length actual)
        (recur (long (+ bytes-per-diff-line offset)))))))

(defn format-binary-delta
  "Formats the delta using [[print-binary-delta]] and returns the result as a string."
  [expected actual]
  (with-out-str
    (print-binary-delta expected actual)))

(ns io.aviso.binary
  "Utilities for formatting binary data (byte arrays) or binary deltas."
  (import (java.lang StringBuilder))
  (require [io.aviso
            [ansi :as ansi]
            [writer :as w]]))

(defprotocol BinaryData
  "Allows various data sources to be treated as a byte-array data type that
  supports a length and random access to individual bytes."

  (data-length [this] "The total number of bytes available.")
  (byte-at [this index] "The byte value at a specific offset."))

(extend-type (Class/forName "[B")
  BinaryData
  (data-length [ary] (alength ary))
  (byte-at [ary index] (aget ary index)))

;;; Extends String as a convenience; assumes that the
;;; String is in utf-8.

(extend-type String
  BinaryData
  (data-length [s] (.length s))
  (byte-at [s index] (-> s (.charAt index) byte)))

(extend-type nil
  BinaryData
  (data-length [this] 0)
  (byte-at [this index] (throw (IndexOutOfBoundsException. "Can't use byte-at with nil."))))

(def ^:private ^:const bytes-per-diff-line 16)
(def ^:private ^:const bytes-per-line (* 2 bytes-per-diff-line))

(defn- write-line
  [writer offset data line-count]
  (w/writef writer "%04X:" offset)
  (doseq [i (range line-count)]
    (w/writef writer " %02X" (byte-at data (+ offset i))))
  (w/write writer w/endline))

(defn write-binary
  "Formats a ByteData into a hex-dump string, consisting of multiple lines; each line formatted as:

  0000: 4E 6F 77 20 69 73 20 74 68 65 20 74 69 6D 65 20 66 6F 72 20 61 6C 6C 20 67 6F 6F 64 20 6D 65 6E
  0020: 20 74 6F 20 63 6F 6D 65 20 74 6F 20 74 68 65 20 61 69 64 20 6F 66 20 74 68 65 69 72 20 63 6F 75
  0040: 6E 74 72 79

  (32 bytes per line)

  ... that is, a four-byte offset, then up-to 32 bytes (depending on the length of the data)."
  ([data]
   (write-binary *out* data))
  ([writer data]
   (loop [offset 0]
     (let [remaining (- (data-length data) offset)]
       (when (pos? remaining)
         (write-line writer offset data (min bytes-per-line remaining))
         (recur (+ bytes-per-line offset)))))))

(defn format-binary
  "Formats the data as with write-binary and returns the result as a string."
  [data]
  (let [result (StringBuilder. (* 3 (data-length data)))]
    (write-binary result data)
    (.toString result)))

(defn- match?
  [byte-offset data-length data alternate-length alternate]
  (and
    (< byte-offset data-length)
    (< byte-offset alternate-length)
    (== (byte-at data byte-offset) (byte-at alternate byte-offset))))

(defn- to-hex
  [byte-array byte-offset]
  ;; This could be made a lot more efficient!
  (format "%02X" (byte-at byte-array byte-offset)))

(defn- write-byte-deltas
  [writer ansi-color pad? offset data-length data alternate-length alternate]
  (doseq [i (range bytes-per-diff-line)]
    (let [byte-offset (+ offset i)]
      (cond
        ;; Exact match on both sides is easy, just print it out.
        (match? byte-offset data-length data alternate-length alternate) (w/writes writer " " (to-hex data byte-offset))
        ;; Some kind of mismatch, so decorate with this side's color
        (< byte-offset data-length) (w/writes writer " " (ansi-color (to-hex data byte-offset)))
        ;; Are we out of data on this side?  Print a "--" decorated with the color.
        (< byte-offset alternate-length) (w/writes writer " " (ansi-color "--"))
        ;; This side must be longer than the alternate side.
        ;; On the left/green side, we need to pad with spaces
        ;; On the right/red side, we need nothing.
        pad? (w/write writer "   ")))))

(defn- write-delta-line
  [writer offset expected-length ^bytes expected actual-length actual]
  (let [line-count (max (min bytes-per-diff-line (- expected-length offset))
                        (min bytes-per-diff-line (- actual-length offset)))]
    (w/writef writer "%04X:" offset)
    (write-byte-deltas writer ansi/bold-green true offset expected-length expected actual-length actual)
    (write-byte-deltas writer ansi/bold-red false offset actual-length actual expected-length expected)
    (w/write writer w/endline)))

(defn write-binary-delta
  "Formats a hex dump of the expected data (on the left) and actual data (on the right). Bytes
  that do not match are highlighted in green on the expected side, and red on the actual side.
  When one side is shorter than the other, it is padded with -- placeholders to make this
  more clearly visible.

  expected - ByteData
  actual - ByteData

  Display 16 bytes (from each data set) per line."
  ([expected actual] (write-binary-delta *out* expected actual))
  ([writer expected actual]
   (let [expected-length (data-length expected)
         actual-length (data-length actual)
         target-length (max actual-length expected-length)]
     (loop [offset 0]
       (when (pos? (- target-length offset))
         (write-delta-line writer offset expected-length expected actual-length actual)
         (recur (+ bytes-per-diff-line offset)))))))

(defn format-binary-delta
  "Formats a hex dump of the expected data (on the left) and actual data (on the right). Bytes
  that do not match are highlighted in green on the expected side, and red on the actual side.
  When one side is shorter than the other, it is padded with -- placeholders to make this
  more clearly visible.

  expected - ByteData
  actual - ByteData

  Display 16 bytes (from each data set) per line."
  [expected actual]
  (let [result (StringBuilder. 2000)]
    (write-binary-delta result expected actual)
    (.toString result)))
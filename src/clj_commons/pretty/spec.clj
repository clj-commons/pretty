(ns clj-commons.pretty.spec
  (:require [clojure.spec.alpha :as s]
            [clj-commons.ansi :as ansi]
            [clj-commons.pretty.annotations :as ann]))

(s/def ::nonneg-integer (s/and integer? #(<= 0 %)))

(s/def ::positive-integer (s/and integer? pos?))


(s/def ::single-character (s/or
                            :char char?
                            :string (s/and string?
                                           #(= 1 (count %)))))

;; clj-commons.ansi:

(s/def ::ansi/composed-string (s/or
                                :string string?
                                :nil nil?
                                :span ::ansi/span
                                :sequential ::ansi/composed-strings
                                :other any?))

(s/def ::ansi/span (s/and vector?
                          (s/cat
                            :font ::ansi/span-font
                            :span-body (s/* ::ansi/composed-string))))

(s/def ::ansi/span-font (s/or
                          :nil nil?
                          :font ::ansi/font
                          :full ::ansi/span-font-full))

(s/def ::ansi/font (s/or
                     :simple keyword?
                     :list (s/coll-of keyword? :kind vector?)))

(s/def ::ansi/span-font-full (s/keys
                               :opt-un [::ansi/font ::ansi/width ::ansi/pad]))

(s/def ::ansi/width ::positive-integer)

(s/def ::ansi/pad #{:left :right :both})

(s/def ::ansi/composed-strings (s/and sequential?
                                      (s/* ::ansi/composed-string)))

(s/fdef ansi/compose
        :args (s/* ::ansi/composed-string)
        :ret string?)

(s/fdef ansi/pout
        :args (s/* ::ansi/composed-string)
        :ret nil?)

(s/fdef ansi/pcompose                                       ; old name of pout
        :args (s/* ::ansi/composed-string)
        :ret nil?)

(s/fdef ansi/perr
        :args (s/* ::ansi/composed-string)
        :ret nil?)

;; clj-commons.pretty.annotations

(s/fdef ann/callouts
        :args (s/cat
                :style (s/? ::ann/style)
                :annotations (s/+ ::ann/annotation))
        :req (s/coll-of ::ansi/composed-string))

(s/def ::ann/style (s/keys :req-un [::ansi/font
                                    ::ann/compact?
                                    ::ann/marker
                                    ::ann/bar
                                    ::ann/nib]))

(s/def ::ann/compact? boolean?)
(s/def ::ann/marker ::single-character)
(s/def ::ann/bar ::single-character)
(s/def ::ann/nib ::ansi/composed-string)

(s/def ::ann/annotation (s/keys :req-un [::ann/message
                                         ::ann/offset]
                                :opt-un [::ann/length
                                         ::ansi/font]))

(s/def ::ann/message ::ansi/composed-string)
(s/def ::ann/offset ::nonneg-integer)
(s/def ::ann/length ::positive-integer)

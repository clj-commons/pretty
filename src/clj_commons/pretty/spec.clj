(ns clj-commons.pretty.spec
  (:require [clojure.spec.alpha :as s]
            [clj-commons.ansi :as ansi]
            [clj-commons.pretty.annotations :as ann]))

(s/def ::nonneg-integer (s/and integer? #(<= 0 %)))

(s/def ::positive-integer (s/and integer? pos?))


(s/def ::single-character (s/and string?
                                 #(= 1 (count %))))

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
                :annotations ::ann/annotations)
        :ret (s/coll-of ::ansi/composed-string))

(s/def ::ann/style (s/keys :req-un [::ansi/font
                                    ::ann/spacing
                                    ::ann/marker
                                    ::ann/bar
                                    ::ann/nib]))

(s/def ::ann/spacing #{:tall :compact :minimal})
(s/def ::ann/marker (s/or
                      :basic ::single-character
                      :fn fn?
                      :tri (s/and string? #(= 3 (count %)))))

(s/def ::ann/bar ::single-character)
(s/def ::ann/nib ::ansi/composed-string)

(s/def ::ann/annotations (s/coll-of ::ann/annotation))

(s/def ::ann/annotation (s/keys :req-un [::ann/message
                                         ::ann/offset]
                                :opt-un [::ann/length
                                         ::ansi/font
                                         ::ann/marker]))

(s/def ::ann/message ::ansi/composed-string)
(s/def ::ann/offset ::nonneg-integer)
(s/def ::ann/length ::positive-integer)

(s/def ::ann/annotate-lines-opts (s/keys :opt-un [::ann/style
                                                  ::ann/start-line
                                                  ::ann/line-number-width]))

(s/def ::ann/line-number-width ::positive-integer)
(s/def ::ann/start-line ::positive-integer)

(s/def ::ann/lines (s/coll-of ::ann/line-data))

(s/def ::ann/line-data (s/keys :req-un [::ann/line]
                               :opt-un [::ann/annotations]))

(s/def ::ann/line ::ansi/composed-string)

(s/fdef ann/annotate-lines
        :args (s/cat
                :opts (s/? (s/nilable ::ann/annotate-lines-opts))
                :lines ::ann/lines)
        :ret (s/coll-of ::ansi/composed-string))

; Copyright 2024 Nubank NA
;
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0)
; which can be found in the file epl-v10.html at the root of this distribution.
;
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
;
; You must not remove this notice, or any other, from this software.

;; NOTE: This code briefly originated in io.pedestal/pedestal, which uses the EPL
;; license.

(ns clj-commons.format.table.specs
  (:require [clojure.spec.alpha :as s]
            [clj-commons.format.table :refer [print-table]]))

(s/fdef print-table
        :args (s/cat :columns
                     (s/or :columns ::columns
                           :opts ::options)
                     :data (s/coll-of map?)))

(s/def ::options (s/keys :req-un [::columns]
                         :opt-un [::style
                                  ::default-decorator
                                  ::row-annotator]))

(s/def ::row-annotator
  (s/fspec
    :args (s/cat
            :index int?
            :value any?)
    :ret any?))

;; Not a lot of gain for breaking down what's in a style map.
(s/def ::style map?)

(s/def ::columns (s/coll-of ::column))
(s/def ::column
  (s/or :simple keyword?
        :full ::column-full))

(s/def ::column-full
  (s/keys :req-un [::key]
          :opt-un [::title
                   ::width
                   ::decorator
                   ::pad
                   ::align
                   ::title-pad
                   ::title-align]))

(s/def ::align #{:right :left :center})
(s/def ::pad #{:left :right :both})
(s/def ::title-pad ::pad)
(s/def ::title-align ::align)

(s/def ::key ifn?)
(s/def ::title string?)
(s/def ::width (s/and int? pos?))
(s/def ::font-declaration (s/or
                             :keyword keyword?
                             :vector (s/coll-of (s/nilable keyword?)
                                                :kind vector?)))
(s/def ::decorator (s/fspec
                     :args (s/cat
                             :index int?
                             :value any?)
                     :ret (s/nilable ::font-declarationse)))
(s/def ::default-decorator ::decorator)

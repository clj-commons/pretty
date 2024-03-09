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
            [clj-commons.format.table.specs :refer [format-table]]))

(s/fdef print-table
        :args (s/cat :columns
                     (s/or :columns ::columns
                           :opts ::options)
                     :data (s/coll-of map?)))

(s/def ::options (s/keys :req-un [::columns]
                         :opt-un [::style
                                  ::default-decorator]))

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
                   ::pad]))

(s/def ::pad #{:left :right})

(s/def ::key ifn?)
(s/def ::title string?)
(s/def ::width (s/and int? pos?))
(s/def ::decorator (s/fspec
                     :args (s/cat
                             :index int?
                             :value any?)
                     :ret (s/nilable keyword?)))
(s/def ::default-decorator ::decorator)
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

(ns clj-commons.format.table
  "Formatted tablular output.

  Specs are in [[clj-commons.format.table.specs]]."
  {:added "2.3"}
  (:require [clojure.string :as string]
            [clj-commons.ansi :refer [compose]]))

(defn- bar
  [width]
  (let [b (StringBuilder. (int width))]
    (while (< (.length b) width)
      (.append b "━"))
    (.toString b)))

(defn- default-title
  [key]
  (-> key name (string/replace "-" " ") string/capitalize))

(defn- expand-column
  [column]
  (cond
    (keyword? column)
    {:key   column
     :title (default-title column)}

    (-> column :title nil?)
    (assoc column
      :title (-> column :key default-title))

    :else
    column))

(defn- set-width
  [column data]
  (let [{:keys [key ^String title]} column
        title-width (.length title)
        width (->> data
                   (map key)
                   (map str)
                   (map #(.length %))
                   (reduce max title-width))]
    (assoc column :width width)))

(defn print-table
  "Similar to clojure.pprint/print-table, but with fancier graphics and more control
  over column titles.

  The rows are a seq of associative values, usually maps.

  In simple mode, each column is just a keyword; the column title is derived
  from the keyword, and the column's width is set to the maximum
  of the title width and the width of the longest value in the rows.

  Alternately, a column can be a map with keys :key and :title, and optional
  keys :width and :decorator.

  With a map, the :key can be any function that, passed a single row,
  returns the printable value; this will ultimately be passed to
  [[compose]], so it can feature fonts and padding ... but when doing so,
  be sure to specify the actual :width.

  Note that when :key is not a function, :title must be specified explicitly.

  The decorator, if present, is a function; it will be
  passed the row index and the value for the column,
  and returns a font keyword (or nil)."
  [columns rows]
  (let [columns' (->> columns
                      (map expand-column)
                      (map #(set-width % rows))
                      (map-indexed #(assoc %2 :index %1)))
        last-column (dec (count columns'))]
    (print "┏━")
    (doseq [{:keys [width index]} columns']
      (print (bar width))
      (when-not (= index last-column)
        (print "━┳━")))
    (println "━┓")

    (print "┃")
    (doseq [{:keys [width title]} columns']
      (print (compose " "
                      [{:width width
                        :pad   :right
                        :font  :bold} title]
                      " ┃")))
    (println)
    (print "┣━")
    (doseq [{:keys [width index]} columns']
      (print (bar width))
      (when-not (= index last-column)
        (print "━╋━")))
    (println "━┫")

    (when (seq rows)
      (loop [[datum & more-data] rows
             row-index 0]
        (print "┃")
        (doseq [{:keys [width key decorator]} columns'
                :let [value (get datum key)
                      font (when decorator
                             (decorator row-index value))]]
          (print (compose
                   " "
                   [{:font  font
                     :pad   :right
                     :width width}
                    (get datum key)]
                   " ┃")))
        (println)
        (when more-data
          (recur more-data (inc row-index)))))

    (print "┗━")
    (doseq [{:keys [width index]} columns']
      (print (bar width))
      (when-not (= index last-column)
        (print "━┻━")))
    (println "━┛")))



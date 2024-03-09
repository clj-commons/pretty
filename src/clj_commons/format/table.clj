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
  "Formatted tabular output, similar to (but much prettier and more flexible than)
   clojure.pprint/print-table.

  Specs are in [[clj-commons.format.table.specs]]."
  {:added "2.3"}
  (:require [clojure.string :as string]
            [clj-commons.ansi :refer [pcompose]]))

(defn- make-bar
  [width s]
  (let [b (StringBuilder. (int width))]
    (while (< (.length b) width)
      (.append b s))
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
  (let [{:keys [key ^String title width]} column
        title-width (.length title)
        width' (if width
                 (max width title-width)
                 (->> data
                      (map key)
                      (map str)
                      (map #(.length %))
                      (reduce max title-width)))]
    (assoc column :width width')))

(def default-style
  "Default style, with thick borders (using character graphics) and a header and footer."
  {:hbar          "━"
   :header?       true
   :header-left   "┏━"
   :header-sep    "━┳━"
   :header-right  "━┓"
   :divider-left  "┣━"
   :divider-sep   "━╋━"
   :divider-right "━┫"
   :row-left      "┃ "
   :row-sep       " ┃ "
   :row-right     " ┃"
   :footer?       true
   :footer-left   "┗━"
   :footer-sep    "━┻━"
   :footer-right  "━┛"})

(def skinny-style
  "Removes most of the borders and uses simple characters for column separators."
  {:hbar          "-"
   :header?       false
   :divider-left  nil
   :divider-sep   "-+-"
   :divider-right nil
   :row-left      nil
   :row-sep       " | "
   :row-right     nil
   :footer?       false})

(defn print-table
  "Similar to clojure.pprint/print-table, but with fancier graphics and more control
  over column titles.

  The rows are a seq of associative values, usually maps.

  In simple mode, each column is just a keyword; the column title is derived
  from the keyword, and the column's width is set to the maximum
  of the title width and the width of the longest value in the rows.

  Alternately, a column can be a map with keys :key and :title, and optional
  keys :width and :decorator, and :align.

  With a map, the :key can be any function that, passed a single row,
  returns the printable value; this will ultimately be passed to
  [[compose]], so it can feature fonts and padding ... but when doing so,
  be sure to specify the actual :width.

  Note that when :key is not a function, :title must be specified explicitly.

  The decorator, if present, is a function; it will be
  passed the row index and the value for the column,
  and returns a font keyword (or nil).

  By default, columns are padded on the left, except for the final column
  which pads on the right; the :align key can be :left or :right to override this
  (affecting both the header row and each data row).

  opts can be a seq of columns, or it can be a map
  with keys :columns (required, a seq of columns),
  :style (optional, overrides the output style),
  and :default-decorator.

  For :style, there's the [[default-style]] which uses thicker blocks, and
  the [[skinny-style]] which is simpler.

  The :default-decorator, if provided, is used for all columns that do not
  have a specific decorator."
  [opts rows]
  (let [opts' (if (sequential? opts)
                {:columns opts}
                opts)
        {:keys [columns style default-decorator]
         :or   {style default-style}} opts'
        {:keys [header?
                footer?
                header-left
                header-sep
                header-right
                divider-left
                divider-sep
                divider-right
                row-left
                row-sep
                row-right
                footer-left
                footer-sep
                footer-right
                hbar]} style
        last-column-index (dec (count columns))
        columns' (->> columns
                      (map expand-column)
                      (map #(set-width % rows))
                      (map-indexed #(assoc %2 :index %1))
                      (map (fn [col]
                             (assoc col
                               :last? (= last-column-index (:index col))
                               :bar (make-bar (:width col) hbar)))))]
    (when header?
      (pcompose
        header-left
        (for [{:keys [last? bar]} columns']
          (list bar
                (when-not last?
                  header-sep)))
        header-right))

    (pcompose
      row-left
      (for [{:keys [width title last? pad]} columns']
        (list [{:width width
                :pad   (or pad (if last? :right :left))
                :font  :bold} title]
              (when-not last?
                row-sep)))
      row-right)

    (pcompose
      divider-left
      (for [{:keys [bar last?]} columns']
        (list bar
              (when-not last?
                divider-sep)))
      divider-right)

    (when (seq rows)
      (loop [[datum & more-data] rows
             row-index 0]
        (pcompose
          row-left
          (for [{:keys [width key decorator last? pad]} columns'
                :let [value (get datum key)
                      decorator' (or decorator default-decorator)
                      font (when decorator'
                             (decorator' row-index value))]]
            (list [{:font  font
                    :pad   (or pad (if last? :right :left))
                    :width width}
                   (get datum key)]
                  (when-not last?
                    row-sep)))
          row-right)
        (when more-data
          (recur more-data (inc row-index)))))

    (when footer?
      (print footer-left)
      (doseq [{:keys [bar last?]} columns']
        (print bar)
        (when-not last?
          (print footer-sep)))
      (println footer-right))))

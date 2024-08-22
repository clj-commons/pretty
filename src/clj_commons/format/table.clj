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

  Alternately, a column can be a map:

  Key        | Type                 | Description
  ---        |---                   |---
  :key       | keyword/function     | Passed the row data and returns the value for the column (required)
  :title     | String               | The title for the column
  :title-pad | :left, :right, :both | How to pad the title column; default is :both to center the title
  :width     | number               | Width of the column
  :decorator | function             | May return a font declaration for the cell
  :pad       | :left, :right, :both | Defaults to :left except for last column, which pads on the right

  :key is typically a keyword but can be an arbitrary function
  (in which case, you must also provide :title). The return
  value is a composed string (passed to [[compose]]); if returning a composed string,
  you must also provide an explicit :width.

  The default for :title is deduced from :key; when omitted and :key is a keyword;
  the keyword is converted to a string, capitalized, and embedded dashes
  converted to spaces.

  :width will be determined as the maximum width of the title or of any
  value in the data.

  The decorator is a function; it will be
  passed the row index and the value for the column,
  and returns a font declaration (or nil).  A font declaration can be a single keyword
  (.e.g, :red.bold) or a vector of keywords (e.g. [:red :bold]).

  opts can be a seq of columns, or it can be a map of options:

  Key                | Type           | Description
  ---                |---             |---
  :columns           | seq of columns | Describes the columns to print
  :style             | map            | Overrides the default styling of the table
  :default-decorator | function       | Used when a column doesn't define it own decorator
  :row-annotator     | function       | Can add text immediately after the end of the row

  :default-decorator is only used for columns that do not define their own
  decorator.  This can be used, for example, to alternate the background color
  of cells.

  The :row-annotator is passed the row index and the row data,
  and returns a composed string that is appended immediately after
  the end of the row (but outside any border), which can be used to
  add a note to the right of a row."
  [opts rows]
  (let [opts' (if (sequential? opts)
                {:columns opts}
                opts)
        {:keys [columns style default-decorator row-annotator]
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
      (for [{:keys [width title title-pad last?]} columns']
        (list [{:width width
                :pad   (or title-pad :both)
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
      (loop [[row & more-rows] rows
             row-index 0]
        (pcompose
          row-left
          (for [{:keys [width key decorator last? pad]} columns'
                :let [value (key row)
                      decorator' (or decorator default-decorator)
                      font (when decorator'
                             (decorator' row-index value))]]
            (list [{:font  font
                    :pad   (or pad (if last? :right :left))
                    :width width}
                   value]
                  (when-not last?
                    row-sep)))
          row-right
          (when row-annotator
            (row-annotator row-index row)))
        (when more-rows
          (recur more-rows (inc row-index)))))

    (when footer?
      (print footer-left)
      (doseq [{:keys [bar last?]} columns']
        (print bar)
        (when-not last?
          (print footer-sep)))
      (println footer-right))))


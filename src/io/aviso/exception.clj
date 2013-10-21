(ns io.aviso.exception
  "Code to assist with presenting exceptions in pretty way."
  (:import (java.lang StringBuilder))
  (use io.aviso.ansi)
  (import [clojure.lang Compiler])
  (require [clojure
            [set :as set]
            [string :as str]]))

;;; Obviously, this is making use of some internals of Clojure that
;;; could change at any time.

(def ^:private clojure->java
  (->> (Compiler/CHAR_MAP)
       set/map-invert
       (sort-by #(-> % first .length))
       reverse))

(defn- match-mangled
  [^String s i]
  (->> clojure->java
       (filter (fn [[k _]] (.regionMatches s i k 0 (.length k))))
       ;; Return the matching sequence and its single character replacement
       first))

(defn demangle
  "De-munges a Java name back to a Clojure name by converting mangled sequences, such as \"_QMARK_\"
  back into simple characters."
  [s]
  (let [in-length (.length s)
        result (StringBuilder. in-length)]
    (loop [i 0]
      (cond
        (>= i in-length) (.toString result)
        (= \_ (.charAt s i)) (let [[match replacement] (match-mangled s i)]
                               (.append result replacement)
                               (recur (+ i (.length match))))
        :else (do
                (.append result (.charAt s i))
                (recur (inc i)))))))

(defn- match-keys
  "Apply the function f to all values in the map; where the result is truthy, add the key to the result."
  [m f]
  ;; (seq m) is necessary because the source is via (bean), which returns an odd implementation of map
  (reduce (fn [result [k v]] (if (f v) (conj result k) result)) [] (seq m)))

(defn- expand-exception
  [exception]
  (let [properties (bean exception)
        cause (:cause properties)
        nil-property-keys (match-keys properties nil?)
        throwable-property-keys (match-keys properties #(.isInstance Throwable %))
        remove' #(remove %2 %1)
        nested-exception (-> properties
                             (select-keys throwable-property-keys)
                             vals
                             (remove' nil?)
                             ;; Avoid infinite loop!
                             (remove' #(= % exception))
                             first)
        ;; Ignore basic properties of Throwable, any nil properties, and any properties
        ;; that are themselves Throwables
        discarded-keys (concat [:suppressed :message :localizedMessage :class :stackTrace]
                               nil-property-keys
                               throwable-property-keys)
        retained-properties (apply dissoc properties discarded-keys)]
    [{:exception exception
      :properties retained-properties}
     nested-exception]))

(defn analyze-exception
  "Converts an exception into a seq of maps representing nested exceptions. Each map
  contains the original exception as :exception, plus a :properties map of additional properties
  on the exception that should be reported.

  The :properties map does not include any properties that are assignable to type Throwable.
  The first property of type Throwable (not necessarily the rootCause property)
  will be used as the nested exception (for the next map in the sequence).

  The final map in the sequence will have an additional value, :root, set to true."
  [^Throwable t]
  (loop [result []
         current t]
    (let [[expanded nested] (expand-exception current)]
      (if nested
        (recur (conj result expanded) nested)
        (conj result (assoc expanded :root true))))))

(defn- max-length
  "Find the maximum length of the strings in the collection."
  [coll]
  (if (empty? coll)
    0
    (apply max (map #(-> % .length) coll))))

(defn- max-value-length
  [coll key]
  (max-length (map key coll)))

(defn- append!
  [^StringBuilder builder & values]
  (doseq [v values] (.append builder v)))

(defn- indent! [builder spaces]
  (append! builder (apply str (repeat spaces \space))))

(defn- justified!
  ([builder width value]
   (indent! builder (- width (.length value)))
   (append! builder value))
  ([builder width prefix value suffix]
   (indent! builder (- width (.length value)))
   (append! builder prefix value suffix)))

(defn- update-keys [m f]
  "Builds a map where f has been applied to each key in m."
  (into {} (map (fn [[k v]] [(f k) v]) m)))

(defn- convert-to-clojure
  [class-name]
  (let [[namespace-name & raw-function-ids] (str/split class-name #"\$")
        ;; Clojure adds __1234 unique ids to the ends of things, remove those.
        function-ids (map #(or (nth (first (re-seq #"([\w|.|-]+)__\d+?" %)) 1 nil) %) raw-function-ids)]
    ;; The assumption is that no real namespace or function name will contain underscores (the underscores
    ;; are name-mangled dashes).
    (->> (cons namespace-name function-ids) (map demangle))))

(defn- expand-stack-trace
  [element]
  (let [class-name (.getClassName element)
        method-name (.getMethodName element)
        file-name (or (.getFileName element) "")
        is-clojure? (and (.endsWith file-name ".clj")
                         (contains? #{"invoke" "doInvoke"} method-name))
        names (if is-clojure? (convert-to-clojure class-name) [])
        name (str/join "/" names)]
    {:file file-name
     :line (-> element .getLineNumber str)
     :class class-name
     :method method-name
     ;; Used to calculate column width
     :name name
     ;; Used to present compound name with last term highlighted
     :names names}))

(def ^:dynamic *fonts*
  "ANSI fonts for different elements in the formatted exception report."
  {:exception (str bold-font red-font)
   :reset reset-font
   :message italic-font
   :property bold-font
   :function-name bold-font})

(defn- format-stack-trace!
  [builder stack-trace]
  (let [elements (map expand-stack-trace stack-trace)
        file-width (max-value-length elements :file)
        line-width (max-value-length elements :line)
        name-width (max-value-length elements :name)
        class-width (max-value-length elements :class)]
    (doseq [{:keys [file line name names class method]} elements]
      (indent! builder (- name-width (.length name)))
      ;; There will be 0 or 2+ names (the first being the namespace)
      (when-not (empty? names)
        (doto builder
          (append! (->> names drop-last (str/join "/")))
          (append! "/" (:function-name *fonts* "") (last names) (:reset *fonts* ""))))
      (doto builder
        (append! "  ")
        (justified! file-width file)
        (append! ":")
        (justified! line-width line)
        (append! "  ")
        (justified! class-width class)
        (append! "." method \newline)))))

(defn format-exception
  "Formats an exception as a multi-line string. The *fonts* var contains ANSI definitions for how fonts
  are displayed; bind it to nil to remove ANSI formatting entirely."
  [exception]
  (let [exception-font (:exception *fonts* "")
        message-font (:message *fonts* "")
        property-font (:property *fonts* "")
        reset-font (:reset *fonts* "")
        exception-stack (->> exception
                             analyze-exception
                             (map #(assoc % :name (-> % :exception class .getName))))
        exception-column-width (max-value-length exception-stack :name)
        result (StringBuilder. 2000)]
    (doseq [e exception-stack]
      (justified! result exception-column-width exception-font (:name e) reset-font)
      ;; TODO: Handle no message for the exception specially
      (append! result ": "
               message-font
               (-> e :exception .getMessage)
               reset-font
               \newline)

      (let [properties (update-keys (:properties e) name)
            prop-keys (keys properties)
            ;; Allow for the width of the exception class name, and some extra
            ;; indentation.
            prop-name-width (+ exception-column-width
                               4
                               (max-length prop-keys))]
        (doseq [k (sort prop-keys)]
          (justified! result prop-name-width property-font k reset-font)
          (append! result ": " (get properties k) \newline))
        (if (:root e)
          (format-stack-trace! result (-> e :exception .getStackTrace)))))
    (.toString result)))